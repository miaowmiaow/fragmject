/**
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 附加习性动作：好奇绕圈、原地打转、三段冲刺。
 *
 * 原项目没有这段时间的行为，是为了"看起来像活物"新增的。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * 动作层需要的最小驱动能力（由 [FishSwimRuntime] 实现）。
 *
 * 动作只决定「去哪、多快」，积分与边界仍由运行时统一处理：
 * 新增动作不需要碰物理代码，也不必把运行时的私有状态全部暴露出去。
 */
internal interface FishMover {
    val bodyCenter: Offset
    var navTarget: Offset
    var navHeading: Float
    var navSpeed: Float

    fun steerTo(towards: Offset, dtSeconds: Float, fastTurn: Boolean)

    fun drive(desiredSpeed: Float, dtSeconds: Float, rate: Float)
}

/**
 * 附加习性动作（同一时刻只有一个生效，优先级见 [update]）：
 *
 * | 动作 | 触发 | 观感 |
 * |---|---|---|
 * | 好奇绕圈 | 到点后约 25% | 绕着刚才到的地方转小半圈，"有点兴趣" |
 * | 原地打转 | 到点后约 10% | 线速度很低、角速度拉满，像"找不到方向" |
 * | 三段冲刺 | 到点后小概率 | 冲—滑—冲—滑—冲，不是一条直线加速 |
 *
 * 追手指刻意不做：它需要 `:app` 把手势坐标一路传下来，而常驻宠物跟手会干扰阅读类页面。
 */
internal class FishActionModes(
    private val mover: FishMover,
    private val headRadiusPx: Float,
) {
    private var curiousUntil = 0f
    private var curiousCenter = Offset.Zero
    private var curiousAngle = 0f

    private var spinUntil = 0f

    private var dashStage = -1
    private var dashSwitchAt = 0f

    // ------------------------------------------------------------ 触发

    fun startCurious(elapsed: Float, center: Offset, heading: Float) {
        curiousUntil = elapsed + CURIOUS_SECONDS
        curiousCenter = center
        curiousAngle = heading
    }

    fun startSpin(elapsed: Float) {
        spinUntil = elapsed + SPIN_SECONDS
    }

    fun startDash(elapsed: Float) {
        dashStage = 0
        dashSwitchAt = elapsed + DASH_BURST
    }

    // ------------------------------------------------------------ 每帧

    /** 返回 true 表示本帧由某个动作接管了移动（调用方跳过巡航逻辑）。 */
    fun update(elapsed: Float, dtSeconds: Float, maxSpeed: Float): Boolean = when {
        elapsed < curiousUntil -> {
            curious(dtSeconds, maxSpeed)
            true
        }

        elapsed < spinUntil -> {
            spin(dtSeconds, maxSpeed)
            true
        }

        dashStage >= 0 -> {
            dash(elapsed, dtSeconds, maxSpeed)
            true
        }

        else -> false
    }

    /** 好奇：绕着触发点转 2.4s，半径 3.2R。 */
    private fun curious(dtSeconds: Float, maxSpeed: Float) {
        curiousAngle += CURIOUS_DEGREES_PER_SECOND * dtSeconds
        val radians = Math.toRadians(curiousAngle.toDouble())
        mover.navTarget = curiousCenter +
            Offset(cos(radians).toFloat(), sin(radians).toFloat()) * (headRadiusPx * 3.2f)
        mover.steerTo(mover.navTarget, dtSeconds, fastTurn = false)
        mover.drive(maxSpeed * 0.5f, dtSeconds, maxSpeed * 4f)
    }

    /** 原地打转：直接改朝向，线速度只保留 20%。 */
    private fun spin(dtSeconds: Float, maxSpeed: Float) {
        mover.navHeading += SPIN_DEGREES_PER_SECOND * dtSeconds
        mover.navTarget = mover.bodyCenter + FishGeometry.direction(mover.navHeading) * (headRadiusPx * 2f)
        mover.drive(maxSpeed * 0.2f, dtSeconds, maxSpeed * 4f)
    }

    /** 三段冲刺：爆发 0.18s / 滑行 0.13s 交替 6 段，沿当前朝向直冲。 */
    private fun dash(elapsed: Float, dtSeconds: Float, maxSpeed: Float) {
        val bursting = dashStage % 2 == 0
        mover.navTarget = mover.bodyCenter + FishGeometry.direction(mover.navHeading) * (headRadiusPx * 6f)
        mover.steerTo(mover.navTarget, dtSeconds, fastTurn = false)
        mover.drive(if (bursting) maxSpeed * 1.9f else maxSpeed * 0.6f, dtSeconds, maxSpeed * 7f)
        if (elapsed >= dashSwitchAt) {
            dashStage += 1
            dashStage = if (dashStage >= DASH_STAGES) -1 else dashStage
            dashSwitchAt = elapsed + if (dashStage >= 0 && dashStage % 2 == 0) DASH_BURST else DASH_GLIDE
        }
    }

    private companion object {
        const val CURIOUS_SECONDS = 2.4f
        const val CURIOUS_DEGREES_PER_SECOND = 210f
        const val SPIN_SECONDS = 1.3f
        const val SPIN_DEGREES_PER_SECOND = 430f
        const val DASH_BURST = 0.18f
        const val DASH_GLIDE = 0.13f
        const val DASH_STAGES = 6
    }
}
