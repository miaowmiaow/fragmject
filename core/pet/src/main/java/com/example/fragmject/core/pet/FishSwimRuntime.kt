/**
 * 游动运行时。
 *
 * 原项目 https://github.com/Jichensheng/Fish_2 的运动只有一种：`FishDrawableView.onTouchEvent` 里按手指位置
 * 生成三次贝塞尔，再用 `ObjectAnimator.ofFloat(view, "x", "y", path)` 在 2s 内走完。
 * 这里改成每帧积分的状态机（目标可随时改、速度连续），并补了冲刺、悬停、打盹、
 * 受惊、穿越等习性动作——这些原实现都没有。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.random.Random

/** 点击后短暂加速（「冲过去看看」），每秒衰减多少。 */
private const val URGE_DECAY_PER_SECOND = 1.6f

/** 情绪的衰减速度：受惊拉满后约 5s 回到平静。 */
private const val AROUSAL_DECAY = 0.18f


/** 受惊触发的「附近」半径（R 的倍数）：划过点离鱼太远就不吓它。速度阈值在 `FishPetInput.kt`。 */
private const val SWIPE_RADIUS_R = 8f

/**
 * 游动运行时：所有行为的调度中心。
 *
 * 优先级（同一帧只走一个分支）：
 * `打盹 > 受惊 > 附加动作（绕圈/打转/巡边/三段冲刺）> 点击预判 > 巡航`；
 * 跃起是覆盖层，任何分支都可能被它接管速度与朝向（见 [applyLeapMotion]）。
 *
 * 物理积分统一在 [integrate]，随机只发生在这层，绘制层保持纯粹。
 */
internal class FishSwimRuntime(
    internal val motion: FishMotion,
    internal val metrics: FishMetrics,
    internal val bounds: Size,
) : FishMover {

    private var elapsed = 0f
    private var state = FishSwimState(Offset.Zero, 0f, 0f)
    private var initialized = false

    internal var dartUntil = 0f
    internal var nextDartAt = 1.5f
    internal var hoverUntil = 0f
    internal var cruiseScale = 0.8f

    internal var lastInteractionAt = 0f
    internal var dozeAt = DOZE_MIN_SECONDS + Random.nextFloat() * DOZE_SPAN_SECONDS
    internal var wakeUntil = 0f

    private var anticipateUntil = 0f
    private var teleportArrived = false

    // 受惊时间线（姿态计算需要读，故为 internal）
    internal var freezeUntil = 0f
    internal var startleUntil = 0f
    internal var glanceUntil = 0f
    internal var recoverUntil = 0f
    internal var stretchUntil = 0f
    internal var startleFrom = Offset.Zero

    internal var elapsedSeconds: Float
        get() = elapsed
        set(value) {
            elapsed = value
        }

    internal val leap = FishLeap(motion)
    internal val ghostTrail = FishGhostTrail()
    internal val water = FishWaterFx(metrics.headRadiusPx)
    internal val actions = FishActionModes(this, metrics.headRadiusPx)

    private var lastTurnRate = 0f

    /** 目标点/落点的安全区留白：保证"要去的地方"是看得见的。 */
    internal val marginPx: Float get() = metrics.headRadiusPx * 3f

    /**
     * 运动积分的软边界：允许**中心点**越界这么多。
     *
     * 2.5R ≈ 六成身子在屏幕外：能读出"游出去了"，又不会整条消失让人以为宠物丢了。
     */
    private val swimOvershootPx: Float get() = metrics.headRadiusPx * 2.5f

    // ---------------------------------------------------------------- 输入

    /**
     * 普通点击：**原来的逻辑**——急刹、转头、游过去（不是穿越）。
     *
     * 只有页面切换类点击才走 [onTeleport]，否则鱼会在随便点一下时凭空瞬移。
     */
    fun onTap(position: Offset) {
        lastInteractionAt = elapsed
        if (motion.sleeping) {
            wakeUp(position)
            return
        }
        motion.arousal = (motion.arousal + 0.35f).coerceAtMost(1f)
        motion.target = position
        motion.urge = 1f
        anticipateUntil = elapsed + 0.08f
        water.emit(elapsed, position, 1f)
    }

    /**
     * 穿越：**仅**导航返回栈变化（跳详情、返回、深链）时使用；底部 tab 不穿越。
     *
     * 此时页面本身正在转场，鱼若还慢慢游过去会横穿新页面；直接"淡出—换位—淡入"
     * 既符合"换了页面"的语义，也不会在转场期间和新页面抢注意力。
     */
    fun onTeleport(position: Offset) {
        if (motion.teleportElapsed >= 0f) return
        // 跃起途中被穿越打断：先复位，否则会"先弹起再消失"，还会留下半截影子/拉伸
        leap.cancel()
        if (motion.sleeping) {
            motion.sleeping = false
            dozeAt = DOZE_MIN_SECONDS + Random.nextFloat() * DOZE_SPAN_SECONDS
        }
        motion.teleportTo = FishSpace.clamp(position, bounds, marginPx)
        motion.teleportElapsed = 0f
        teleportArrived = false
        hoverUntil = 0f
        state = state.copy(speed = 0f)
        // 消失处留一圈水纹，读作"从这里被吸走"
        water.emit(elapsed, state.position, 0.8f)
    }

    /**
     * 推进穿越时间轴；返回 true 表示本帧位移由穿越接管。
     *
     * 用「是否跨过 ARRIVE 时刻」而不是比较浮点相等：帧步长不固定，必须判区间。
     */
    private fun advanceTeleport(dtSeconds: Float): Boolean {
        if (motion.teleportElapsed < 0f) return false
        motion.teleportElapsed += dtSeconds
        val elapsedInTeleport = motion.teleportElapsed
        if (!teleportArrived && FishTeleport.hasArrived(elapsedInTeleport)) {
            teleportArrived = true
            // 此刻完全透明，换位置不会被看见
            state = state.copy(position = motion.teleportTo, speed = 0f)
            motion.middle = motion.teleportTo
            water.emit(elapsed, motion.teleportTo, 1f)
        }
        if (FishTeleport.hasFinished(elapsedInTeleport)) {
            motion.teleportElapsed = -1f
            teleportArrived = false
            motion.target = metrics.randomTarget(bounds)
            // 落位后甩一下尾巴，避免"出现在那里就不动了"
            motion.urge = 0.7f
            return false
        }
        return true
    }

    /** 手指快速划过鱼附近：完整受惊序列（僵直 → 逃逸 → 回头 → 恢复）。 */
    fun onFastSwipe(position: Offset) {
        if ((position - state.position).getDistance() > metrics.headRadiusPx * SWIPE_RADIUS_R) return
        lastInteractionAt = elapsed
        if (motion.sleeping) wakeUp(position)
        motion.arousal = 1f
        startleFrom = position
        // 时间线只有一个来源：StartleTimeline（可单测）
        freezeUntil = elapsed + StartleTimeline.FREEZE_SECONDS
        glanceUntil = elapsed + StartleTimeline.GLANCE_START_SECONDS
        startleUntil = elapsed + StartleTimeline.TOTAL_SECONDS
        recoverUntil = startleUntil + RECOVER_SECONDS
        stretchUntil = elapsed + StartleTimeline.STRETCH_SECONDS
        water.emit(elapsed, state.position, scale = 1.2f)
        water.emit(elapsed, state.position, scale = 0.9f, delay = 0.08f)
        water.emit(elapsed, state.position, scale = 1.4f, delay = 0.16f)
    }

    private fun wakeUp(from: Offset) {
        motion.sleeping = false
        lastInteractionAt = elapsed
        dozeAt = DOZE_MIN_SECONDS + Random.nextFloat() * DOZE_SPAN_SECONDS
        motion.arousal = 1f
        wakeUntil = elapsed + 0.5f
        water.emit(elapsed, state.position, scale = 1.6f)
        val away = state.position - from
        motion.target = FishSpace.edgePoint(
            from = state.position,
            direction = if (away.getDistance() < 0.001f) FishSpace.randomDirection() else away,
            bounds = bounds,
            margin = marginPx,
        )
        state = state.copy(speed = 0f)
    }

    // ---------------------------------------------------------------- 每帧

    fun advance(dtSeconds: Float) {
        elapsed += dtSeconds
        if (!initialized) {
            // 从「鱼当前所在位置」续上，而不是从容器中心重新出发：
            // 改大小/速度、旋转屏幕都会重建本运行时（metrics/bounds 变），
            // 若在这里硬置中心，用户会看到鱼突然瞬移到屏幕中间。
            // 恢复的是旧尺寸下的坐标，进分屏/折叠后容器变小需夹回安全区（见 FishSpace.restoreStart）。
            val start = FishSpace.restoreStart(motion.middle, bounds, marginPx)
            state = FishSwimState(start, motion.angle, motion.speed)
            if (motion.target == Offset.Zero) motion.target = metrics.randomTarget(bounds)
            // 与启动图标衔接：**首次**出现（motion 为空，而不是旋转屏幕/改尺寸导致的重建）
            // 时在起点留一圈水纹，读作「启动页里那条鱼刚落进页面」
            if (motion.middle == Offset.Zero) water.emit(elapsed, start, 1.2f)
            initialized = true
        }
        val maxSpeed = metrics.tuning.maxSpeed
        leap.update(dtSeconds, elapsed, water)
        if (leap.consumeFinished()) motion.target = metrics.randomTarget(bounds)
        motion.ripples = water.current(elapsed)
        leap.updateSplash(elapsed)
        motion.arousal = (motion.arousal - dtSeconds * AROUSAL_DECAY).coerceAtLeast(0f)
        motion.urge = (motion.urge - dtSeconds * URGE_DECAY_PER_SECOND).coerceAtLeast(0f)

        val teleporting = advanceTeleport(dtSeconds)
        when {
            teleporting -> {}
            motion.sleeping -> updateSleepingFrame()
            elapsed < startleUntil -> updateStartledFrame(dtSeconds, maxSpeed)
            actions.update(elapsed, dtSeconds, maxSpeed) -> {}
            elapsed < anticipateUntil -> updateAnticipatingFrame(dtSeconds, maxSpeed)
            else -> updateCruiseFrame(dtSeconds)
        }
        if (!teleporting) {
            applyLeapMotion(dtSeconds, maxSpeed)
            integrate(dtSeconds)
        }
        updateVigor(maxSpeed)
        updatePoseFrame(dtSeconds)
        updateExtrasFrame()
        recordHistoryFrame()
    }

    // ---------------------------------------------------------------- 行为

    /** 跃起期间接管水平速度与朝向（悬停时速度为 0，靠起跳爆发获得位移）。 */
    private fun applyLeapMotion(dtSeconds: Float, maxSpeed: Float) {
        if (!leap.isActive) return
        state = state.copy(
            speed = slowTowards(state.speed, leap.desiredSpeed(), maxSpeed * 18f * dtSeconds),
            heading = leap.lockedHeading,
        )
    }

    // ---------------------------------------------------------------- 物理与姿态

    private fun integrate(dtSeconds: Float) {
        val direction = FishGeometry.direction(state.heading)
        val next = state.position + direction * (state.speed * dtSeconds)
        state = state.copy(position = FishSpace.clampOutside(next, bounds, swimOvershootPx))
    }

    private fun updateVigor(maxSpeed: Float) {
        val speedNorm = (state.speed / maxSpeed.coerceAtLeast(1f)).coerceIn(0f, 1.9f)
        val turnNorm = (abs(lastTurnRate) / metrics.tuning.maxTurnRate).coerceIn(0f, 1f)
        var vigor = 0.25f + 0.75f * speedNorm + 0.45f * turnNorm + 0.5f * motion.urge
        if (leap.isActive) vigor *= 0.5f
        if (motion.sleeping) vigor = 0.15f
        motion.vigor = vigor.coerceIn(0f, 1.5f)
        motion.middle = state.position
        motion.angle = state.heading
        motion.speed = state.speed
    }

    /** 按转向模型推进一步（供巡航扩展使用）；顺带记录本帧角速度，供活力计算用。 */
    internal fun stepTowards(towards: Offset, dtSeconds: Float, speedScale: Float): FishSwimStep {
        val step = state.step(towards, dtSeconds, metrics.tuning, speedScale)
        state = step.state
        lastTurnRate = step.turnRate
        return step
    }

    // ---------------------------------------------------------------- FishMover

    override val bodyCenter: Offset get() = state.position

    override var navTarget: Offset
        get() = motion.target
        set(value) {
            motion.target = value
        }

    override var navHeading: Float
        get() = state.heading
        set(value) {
            state = state.copy(heading = value)
        }

    override var navSpeed: Float
        get() = state.speed
        set(value) {
            state = state.copy(speed = value)
        }

    override fun steerTo(towards: Offset, dtSeconds: Float, fastTurn: Boolean) {
        val delta = towards - state.position
        if (delta.getDistance() < HEADING_EPSILON) return
        val desired = FishGeometry.headingOf(delta)
        val turnLimit = (if (fastTurn) 420f else 170f) * dtSeconds
        val diff = FishGeometry.normalizeAngle(desired - state.heading).coerceIn(-turnLimit, turnLimit)
        state = state.copy(heading = FishGeometry.normalizeAngle(state.heading + diff))
        lastTurnRate = diff / dtSeconds.coerceAtLeast(0.001f)
    }

    override fun drive(desiredSpeed: Float, dtSeconds: Float, rate: Float) {
        state = state.copy(speed = slowTowards(state.speed, desiredSpeed, rate * dtSeconds))
    }

    private fun slowTowards(current: Float, desired: Float, maxDelta: Float): Float =
        (current + (desired - current).coerceIn(-maxDelta, maxDelta)).coerceAtLeast(0f)

    private companion object {
        const val HEADING_EPSILON = 0.001f
    }
}
