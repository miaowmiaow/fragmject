/**
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 姿态与特效的每帧计算（相位、透明度、挤压拉伸、抖动、残影采样）。
 *
 * 原实现的相位由 `ValueAnimator(0f → 54000f, 180s, REVERSE)` 推进 `currentValue`，
 * 摆动全靠 `sin(currentValue * k)`；这里改成按当前速度积分拍频，
 * 于是"游得快摆得快"是自然结果，而不是另写一段逻辑。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.sin

/** 受惊后的恢复时长（秒）：尾频从 2× 平滑回落到 1×。 */
internal const val RECOVER_SECONDS = 0.45f

/** 受惊起手的挤压拉伸时长（秒）。 */
internal const val STRETCH_SECONDS = 0.22f

/** 残影在受惊结束后还能多活多久（秒）。 */
internal const val GHOST_LINGER_SECONDS = 0.15f

/** 尾拍频率（次/秒）：悬停约 0.6 次、冲刺约 1.9 次。 */
private const val TAIL_BEATS_BASE = 0.6f
private const val TAIL_BEATS_PER_VIGOR = 0.85f

/**
 * 姿态与特效的每帧计算：相位、不透明度、挤压拉伸、逃逸抖动、残影采样。
 *
 * 用同包扩展函数而不是参数列表：需要的状态有十来个，逐项传参会直接撞 detekt 的 8 参数上限；
 * 抽出来的目的是让 [FishSwimRuntime] 留在 500 行以内，同时把"行为"和"表现"分开读。
 */

/**
 * 推进摆动相位。
 *
 * 受惊时 ×2；受惊结束后 0.45s 内平滑回落，并伴随一次"喘气"起伏——
 * 少了这段回落，鱼会像机器人一样瞬间复位。
 */
internal fun FishSwimRuntime.updatePoseFrame(dtSeconds: Float) {
    if (!motion.sleeping) motion.bobR = 0f

    var beat = TAIL_BEATS_BASE + TAIL_BEATS_PER_VIGOR * motion.vigor
    if (motion.sleeping) beat = 0.30f
    if (elapsedSeconds < startleUntil) beat *= 2f
    if (elapsedSeconds < recoverUntil) {
        val k = ((recoverUntil - elapsedSeconds) / RECOVER_SECONDS).coerceIn(0f, 1f)
        beat *= 1f + k
        if (!motion.sleeping) motion.bobR = 0.06f * k * sin((recoverUntil - elapsedSeconds) * 9f)
    }
    motion.phase = (motion.phase + beat * 360f * dtSeconds) % 360_000f
}

/** 不透明度、挤压拉伸、逃逸抖动。跃起激活时跳过拉伸（跃起自己写这两个值）。 */
internal fun FishSwimRuntime.updateExtrasFrame() {
    // 平静略透（0.88），惊起变实（1.0）
    motion.opacity = 0.88f + 0.12f * motion.arousal

    if (leap.isActive) {
        motion.jitter = Offset.Zero
        return
    }

    if (elapsedSeconds < stretchUntil) {
        val progress = 1f - (stretchUntil - elapsedSeconds) / STRETCH_SECONDS
        val k = sin(PI.toFloat() * progress.coerceIn(0f, 1f))
        motion.stretchLong = 1f + 0.09f * k
        motion.stretchShort = 1f - 0.06f * k
    } else {
        motion.stretchLong = 1f
        motion.stretchShort = 1f
    }

    // 逃逸段的高频抖动（10Hz / 13Hz，幅度 ≤0.12R）
    motion.jitter = if (elapsedSeconds > freezeUntil && elapsedSeconds < glanceUntil) {
        Offset(
            x = metrics.headRadiusPx * 0.12f * sin(elapsedSeconds * 62.8f),
            y = metrics.headRadiusPx * 0.08f * sin(elapsedSeconds * 81.7f + 1f),
        )
    } else {
        Offset.Zero
    }
}

/**
 * 残影：受惊后 0.15s 内取 40~200ms 前的两个快照。
 *
 * 窗口之外直接清空，避免每帧多画两次鱼。
 */
internal fun FishSwimRuntime.recordHistoryFrame() {
    // 只有受惊后的短窗口会读残影：窗口外不记录，省掉每帧一次 Snapshot 分配与队列增删
    if (elapsedSeconds > startleUntil + GHOST_LINGER_SECONDS) {
        if (motion.ghosts.isNotEmpty()) motion.ghosts = emptyList()
        return
    }
    ghostTrail.record(elapsedSeconds, bodyCenter, navHeading, motion.phase)
    motion.ghosts = ghostTrail.current(elapsedSeconds)
}
