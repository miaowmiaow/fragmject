/**
 * 跃起：预备压扁 → 腾空（抛物线上抛 + 放大 + 影子分离）→ 落水（回弹 + 水花）。
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 原项目没有跃起，这是为常驻宠物新增的表现。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.sin

/**
 * 跃起：**预备（压扁蓄力）→ 腾空（抛物线 + 纵向拉伸）→ 落水（压扁回弹 + 水花）**。
 *
 * ============================ 为什么不换形态 ============================
 * 俯视投影下把平躺剪影放大 1.25 倍读不出"离水"，但换成正侧视图会和整体风格撕裂
 * （试过一版竖直姿态，观感不对被否）。最终只用三条线索：
 *
 * 1. **放大**（1 → 1.25 → 1）：俯视下"变近 = 变大"；
 * 2. **影子分离**：影碟留在起跳的水面点，朝固定光源反方向偏移并缩小变淡——
 *    这是"离开水面"最硬的线索，比放大本身更重要；
 * 3. **挤压拉伸 + 水花**：蓄力压扁、落水回弹、起跳/落水各一圈涟漪 + 落水水花。
 *
 * ============================ 位移 ============================
 * 一次跃起是**抛体**：起跳瞬间给一次爆发速度（至少 1.15×巡航，因为到点悬停时速度接近 0），
 * 蓄力与滞空段保持该水平速度，落水后 0.14s 内减到 35% 再由巡航接回。
 * 朝向全程锁定，所以是一条直线弹道，不会"跳着跳着拐弯"。
 */
internal class FishLeap(private val motion: FishMotion) {

    private var startedAt = -1f
    private var origin = Offset.Zero
    private var tookOff = false
    private var landed = false
    private var finished = false
    private var splashStart = -100f
    private var splashCenter = Offset.Zero
    private var takeoffSpeed = 0f

    /** 跃起期间锁定的朝向。 */
    var lockedHeading = 0f
        private set

    /** 跃起已进行的秒数（未跃起时为 0）。 */
    var elapsedSeconds = 0f
        private set

    val isActive: Boolean get() = startedAt >= 0f

    fun start(globalElapsed: Float, center: Offset, heading: Float, speed: Float, maxSpeed: Float) {
        if (isActive) return
        startedAt = globalElapsed
        elapsedSeconds = 0f
        origin = center
        lockedHeading = heading
        takeoffSpeed = maxOf(speed, maxSpeed * TAKEOFF_MIN_RATIO)
        tookOff = false
        landed = false
        finished = false
    }

    /** 每帧推进；相位切换时自行触发水面效果。 */
    fun update(dtSeconds: Float, globalElapsed: Float, water: FishWaterFx) {
        if (!isActive) return
        elapsedSeconds += dtSeconds
        val t = elapsedSeconds
        when {
            t < ANTICIPATE -> anticipate()
            t < ANTICIPATE + AIR -> air((t - ANTICIPATE) / AIR, globalElapsed, water)
            t < TOTAL -> landing((t - ANTICIPATE - AIR) / LAND, globalElapsed, water)
            else -> end()
        }
    }

    /**
     * 被"穿越"打断：立刻复位所有跃起参数。
     *
     * 不做这一步，跃起途中换页（跳详情 / 返回 / 深链）会先看到鱼弹起来、再整条消失，
     * 以及落水后才出现的影子/拉伸残留在原地。
     */
    fun cancel() {
        if (!isActive) return
        startedAt = -1f
        elapsedSeconds = 0f
        finished = false
        splashStart = -100f
        motion.splash = null
        motion.leapScale = 1f
        motion.leapLiftR = 0f
        motion.shadowOffsetR = 0f
        motion.shadowAlpha = 0f
        motion.shadowScale = 1f
        motion.shadowCenter = Offset.Zero
        motion.stretchLong = 1f
        motion.stretchShort = 1f
    }

    /** 落水水花进度（只在 0~0.35s 内非空）。 */
    fun updateSplash(globalElapsed: Float) {
        val progress = (globalElapsed - splashStart) / SPLASH_SECONDS
        motion.splash = if (progress in 0f..1f) FishSplash(splashCenter, progress) else null
    }

    /** 结束那一帧返回 true（调用方据此重新挑目标点）。 */
    fun consumeFinished(): Boolean {
        val value = finished
        finished = false
        return value
    }

    /**
     * 抛体轨迹的水平速度：蓄力与滞空都保持起跳速度（无空气阻力时水平匀速），
     * 落水后快速减到 35%，形成"被水拖住"的收尾。
     */
    fun desiredSpeed(): Float {
        if (!isActive) return 0f
        return if (elapsedSeconds < ANTICIPATE + AIR) {
            takeoffSpeed
        } else {
            val progress = ((elapsedSeconds - ANTICIPATE - AIR) / LAND).coerceIn(0f, 1f)
            takeoffSpeed * (1f - (1f - LANDING_REST_RATIO) * progress)
        }
    }

    /** 预备：下压蓄力，还没离开水面（此时不该有影子与放大）。 */
    private fun anticipate() {
        // 影子锚在起跳点：空中鱼会前移，影子必须留在原处才能读出高度
        motion.shadowCenter = origin
        motion.stretchLong = 1.06f
        motion.stretchShort = 0.90f
        motion.leapScale = 1f
        motion.leapLiftR = 0f
        motion.shadowAlpha = 0f
    }

    /** 腾空：抛物线高度 → 放大/上抛/影子分离；纵向拉伸随高度先增后减。 */
    private fun air(progress: Float, globalElapsed: Float, water: FishWaterFx) {
        if (!tookOff) {
            tookOff = true
            water.emit(globalElapsed, origin, scale = 1.3f)
        }
        val h = 4f * progress * (1f - progress)
        motion.leapScale = 1f + 0.25f * h
        motion.leapLiftR = 0.55f * h
        motion.shadowOffsetR = 0.62f * h
        motion.shadowAlpha = 0.32f * (1f - 0.5f * h)
        motion.shadowScale = 1f - 0.35f * h
        val stretch = 1f + 0.10f * sin(PI.toFloat() * progress)
        motion.stretchLong = stretch
        motion.stretchShort = 1f / stretch
    }

    /** 落水：压扁 0.88 → 1，同时炸开两圈涟漪 + 一簇水花（落在**落点**，不是起跳点）。 */
    private fun landing(progress: Float, globalElapsed: Float, water: FishWaterFx) {
        if (!landed) {
            landed = true
            val point = motion.middle
            water.emit(globalElapsed, point, scale = 1.6f)
            water.emit(globalElapsed, point, scale = 1.15f, delay = 0.07f)
            splashStart = globalElapsed
            splashCenter = point
        }
        motion.leapScale = 1f
        motion.leapLiftR = 0f
        motion.shadowAlpha = 0f
        val squash = 0.88f + 0.12f * progress.coerceIn(0f, 1f)
        motion.stretchLong = 2f - squash
        motion.stretchShort = squash
    }

    private fun end() {
        startedAt = -1f
        elapsedSeconds = 0f
        finished = true
        motion.leapScale = 1f
        motion.leapLiftR = 0f
        motion.shadowAlpha = 0f
        motion.shadowScale = 1f
        motion.stretchLong = 1f
        motion.stretchShort = 1f
        motion.shadowCenter = Offset.Zero
    }

    private companion object {
        const val ANTICIPATE = 0.10f
        const val AIR = 0.44f
        const val LAND = 0.14f
        const val TOTAL = ANTICIPATE + AIR + LAND
        const val SPLASH_SECONDS = 0.35f

        /** 起跳速度下限：1.15 倍巡航，保证 0.44s 滞空能飞出一个多身位。 */
        const val TAKEOFF_MIN_RATIO = 1.15f

        /** 落水后残留速度比例（随后由巡航接回）。 */
        const val LANDING_REST_RATIO = 0.35f
    }
}
