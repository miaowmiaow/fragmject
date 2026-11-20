/**
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 水面与残影特效。
 *
 * 原实现只有点击处的单圈涟漪；这里扩展成多圈槽位（受惊/起跳会连炸）、落水水花与逃逸残影。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset

/** 共享空列表：避免每帧分配（见 `current` 的注释）。 */
private val EMPTY_RIPPLES: List<FishRipple> = emptyList()
private val EMPTY_GHOSTS: List<FishGhost> = emptyList()

/** 一圈水面涟漪。 */
internal data class FishRipple(val center: Offset, val radius: Float, val alpha: Float)

/** 落水水花：从水面点向外辐射几条短线，比涟漪更有冲击力。 */
internal data class FishSplash(val center: Offset, val progress: Float)

/** 残影：受惊逃逸时在身后留 1~2 个淡影，读作"水痕 + 速度感"。 */
internal data class FishGhost(
    val center: Offset,
    val heading: Float,
    val phaseDegrees: Float,
    val alpha: Float,
)

/**
 * 水面特效：固定 3 个槽位轮转，支持「错开时间连炸」——受惊时 0/80/160ms 三圈。
 *
 * 用槽位而不是每帧新建列表：受惊、起跳、落水都会发涟漪，槽位可以避免每帧再分配一批对象。
 */
internal class FishWaterFx(private val headRadiusPx: Float) {

    private val slots = Array(SLOT_COUNT) { Slot(start = -100f, center = Offset.Zero, scale = 1f) }

    fun emit(elapsed: Float, position: Offset, scale: Float, delay: Float = 0f) {
        val slot = slots.minBy { it.start }
        slot.start = elapsed + delay
        slot.center = position
        slot.scale = scale
    }

    /**
     * 当前仍在扩散的涟漪（0~3 圈）。
     *
     * 没有涟漪时返回共享的空列表：这个方法每帧都会被调用，
     * 返回新实例会让快照状态的写入判定失效、白白触发一次绘制失效。
     */
    fun current(elapsed: Float): List<FishRipple> {
        var active: ArrayList<FishRipple>? = null
        slots.forEach { slot ->
            val progress = (elapsed - slot.start) / DURATION_SECONDS
            if (progress in 0f..1f) {
                val list = active ?: ArrayList<FishRipple>(SLOT_COUNT).also { active = it }
                list += FishRipple(
                    center = slot.center,
                    radius = headRadiusPx * 3.2f * slot.scale * progress,
                    alpha = 0.30f * (1f - progress),
                )
            }
        }
        return active ?: EMPTY_RIPPLES
    }

    private class Slot(var start: Float, var center: Offset, var scale: Float)

    private companion object {
        const val SLOT_COUNT = 3
        const val DURATION_SECONDS = 1f
    }
}

/**
 * 残影轨迹：记录最近的位置快照，取 40~200ms 前的两帧画淡影。
 *
 * 只在受惊后的短时间内被读取；正常游动时 [current] 返回空列表，不产生额外绘制。
 */
internal class FishGhostTrail {

    private val history = ArrayDeque<Snapshot>()

    fun record(elapsed: Float, center: Offset, heading: Float, phaseDegrees: Float) {
        history.addLast(Snapshot(elapsed, center, heading, phaseDegrees))
        if (history.size > LIMIT) history.removeFirst()
    }

    /**
     * 从最新快照往回找「年龄 40~200ms」的至多两帧。
     *
     * 用 while + 游标而不是 for + break/continue：后者会触发 detekt 的
     * `LoopWithTooManyJumpStatements`（一段循环里 break/continue 超过一个）。
     */
    fun current(elapsed: Float): List<FishGhost> {
        var ghosts: ArrayList<FishGhost>? = null
        var index = history.lastIndex
        while (index >= 0 && (ghosts?.size ?: 0) < MAX_GHOSTS) {
            val snapshot = history[index]
            val age = elapsed - snapshot.time
            if (age > MAX_AGE_SECONDS) {
                index = -1
            } else {
                if (age >= MIN_AGE_SECONDS) {
                    val list = ghosts ?: ArrayList<FishGhost>(MAX_GHOSTS).also { ghosts = it }
                    list += FishGhost(
                        center = snapshot.center,
                        heading = snapshot.heading,
                        phaseDegrees = snapshot.phase,
                        alpha = 0.18f * (1f - age / MAX_AGE_SECONDS),
                    )
                }
                index -= 1
            }
        }
        return ghosts ?: EMPTY_GHOSTS
    }

    private data class Snapshot(val time: Float, val center: Offset, val heading: Float, val phase: Float)

    private companion object {
        const val LIMIT = 20
        const val MAX_GHOSTS = 2
        const val MIN_AGE_SECONDS = 0.04f
        const val MAX_AGE_SECONDS = 0.20f
    }
}
