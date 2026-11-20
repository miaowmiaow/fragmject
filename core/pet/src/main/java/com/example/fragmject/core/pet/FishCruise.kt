package com.example.fragmject.core.pet

import kotlin.random.Random

/**
 * 巡航、打盹与"到点选择"。
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 原项目只有"手指按下 → 沿贝塞尔走 2s"，没有自主巡航；这里是常驻宠物新增的部分。
 *
 * 从 `FishSwimRuntime` 搬出来（那里已 414 行、逼近规则 28 的 500 行上限）。
 * 用同包扩展函数：这些逻辑只写运行时的移动状态，没必要再包一层类。
 */

/** 随机冲刺（dart）的额外速度倍率与时长区间。 */
internal const val DART_BOOST = 0.75f
private const val DART_MIN_SECONDS = 0.35f
private const val DART_SPAN_SECONDS = 0.45f
private const val DART_INTERVAL_MIN_SECONDS = 3.5f
private const val DART_INTERVAL_SPAN_SECONDS = 4.5f

/** 到达目标后的悬停时间区间：只摆尾不前进，是「像活物」的重要一环。 */
private const val HOVER_MIN_SECONDS = 0.3f
private const val HOVER_SPAN_SECONDS = 0.9f

/** 打盹：无操作多久后入睡（常驻宠物比实验台更克制）。 */
internal const val DOZE_MIN_SECONDS = 30f
internal const val DOZE_SPAN_SECONDS = 30f

/** 在屏幕外时推迟入睡的时长：先游回来再睡，避免"宠物停在屏外打盹"。 */
private const val DOZE_RETRY_SECONDS = 4f

/** 到点后改去"屏幕外"的概率：太低看不到出界，太高会显得宠物一直在往外跑。 */
private const val EDGE_EXCURSION_CHANCE = 0.25f

/** 出界目标离屏幕边缘的距离（R 的倍数）。 */
private const val EDGE_TARGET_RATIO = 1.5f

/**
 * 打盹判定（纯函数）。
 *
 * 单独抽出来的原因：这里有一个**只有在"允许游出屏幕"之后才存在**的边界条件——
 * 鱼恰好游到屏外时到点，若直接入睡，宠物会"消失"半分钟。真机上很难复现，
 * 但一个纯函数用例就能锁死。
 */
internal object DozeRule {

    fun shouldDoze(idleSeconds: Float, dozeAfterSeconds: Float, insideScreen: Boolean): Boolean =
        idleSeconds > dozeAfterSeconds && insideScreen
}

/** 打盹：速度归零，只保留身体轻微上下浮（0.10R、周期约 4s）。 */
internal fun FishSwimRuntime.updateSleepingFrame() {
    navSpeed = 0f
    motion.bobR = 0.10f * kotlin.math.sin(elapsedSeconds * 1.6f)
}

/** 点击后的"急刹"：快速转头 + 把速度压到 0，为随后的冲刺做对比。 */
internal fun FishSwimRuntime.updateAnticipatingFrame(dtSeconds: Float, maxSpeed: Float) {
    steerTo(navTarget, dtSeconds, fastTurn = true)
    navSpeed = approach(navSpeed, 0f, maxSpeed * 7f * dtSeconds)
}

/** 自主巡航：打盹判定 → 随机冲刺 → 悬停 → 按转向模型推进 → 到点选行为。 */
internal fun FishSwimRuntime.updateCruiseFrame(dtSeconds: Float) {
    val idleSeconds = elapsedSeconds - lastInteractionAt
    if (elapsedSeconds >= wakeUntil && idleSeconds > dozeAt) {
        if (DozeRule.shouldDoze(idleSeconds, dozeAt, FishSpace.isInside(bodyCenter, bounds))) {
            motion.sleeping = true
            navSpeed = 0f
            water.emit(elapsedSeconds, bodyCenter, 0.8f)
            return
        }
        // 在屏外：先游回来，几秒后再判定
        dozeAt = idleSeconds + DOZE_RETRY_SECONDS
        navTarget = metrics.randomTarget(bounds)
    }

    if (elapsedSeconds >= nextDartAt) {
        dartUntil = elapsedSeconds + DART_MIN_SECONDS + Random.nextFloat() * DART_SPAN_SECONDS
        nextDartAt = elapsedSeconds + DART_INTERVAL_MIN_SECONDS + Random.nextFloat() * DART_INTERVAL_SPAN_SECONDS
    }
    // 悬停结束 → 换新目标；悬停中目标跟随自身，避免减速时朝刚才那个点「回头」
    if (hoverUntil > 0f && elapsedSeconds >= hoverUntil) {
        hoverUntil = 0f
        navTarget = metrics.randomTarget(bounds)
    }
    if (hoverUntil > 0f) navTarget = bodyCenter

    val boost = if (elapsedSeconds < dartUntil) DART_BOOST else 0f
    val step = stepTowards(navTarget, dtSeconds, cruiseScale + boost + motion.urge)
    if (step.arrived && hoverUntil == 0f && !leap.isActive) onArrivedFrame()
}

/** 到点：先悬停，再按概率选一个行为（判定本身在 ArrivalPlanner，可单测）。 */
internal fun FishSwimRuntime.onArrivedFrame() {
    hoverUntil = elapsedSeconds + HOVER_MIN_SECONDS + Random.nextFloat() * HOVER_SPAN_SECONDS
    navTarget = bodyCenter

    // 到达的是屏幕外的点：不做任何"表演"（绕圈/跃起都在看不见的地方发生），先回屏内
    if (!FishSpace.isInside(bodyCenter, bounds)) {
        cruiseScale = 0.7f
        navTarget = metrics.randomTarget(bounds)
        return
    }

    when (ArrivalPlanner.choose(Random)) {
        ArrivalAction.LEAP ->
            leap.start(elapsedSeconds, bodyCenter, navHeading, navSpeed, metrics.tuning.maxSpeed)

        ArrivalAction.OBSERVE -> hoverUntil = elapsedSeconds + 1f + Random.nextFloat() * 1.2f

        ArrivalAction.CURIOUS -> actions.startCurious(elapsedSeconds, bodyCenter, navHeading)

        ArrivalAction.SPIN -> actions.startSpin(elapsedSeconds)

        ArrivalAction.CRUISE -> {
            cruiseScale = 0.55f + Random.nextFloat() * 0.45f
            // 偶尔故意挑屏幕外的点：鱼会游出去一趟再回来
            navTarget = if (Random.nextFloat() < EDGE_EXCURSION_CHANCE) {
                FishSpace.randomEdgeTarget(bounds, metrics.headRadiusPx * EDGE_TARGET_RATIO)
            } else {
                metrics.randomTarget(bounds)
            }
        }
    }
}

/** 以限加速度逼近目标速度（不降到负值）。 */
private fun approach(current: Float, desired: Float, maxDelta: Float): Float =
    (current + (desired - current).coerceIn(-maxDelta, maxDelta)).coerceAtLeast(0f)
