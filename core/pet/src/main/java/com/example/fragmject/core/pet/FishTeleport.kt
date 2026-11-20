/**
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 页面切换时的"穿越"位移：原地淡出 → 不可见时换位 → 目标点弹入。
 *
 * 原项目是单页演示，不存在页面切换；这部分是为常驻宠物新增的交互。
 */
package com.example.fragmject.core.pet

/**
 * 「穿越」位移：原地淡出 → 不可见的一瞬换位置 → 在目标点淡入并弹一下。
 *
 * ============================ 只用于「页面切换类点击」============================
 * 普通点击仍然走原来的"游过去"：用户就是要看鱼游到那儿。
 * 但跳详情 / 返回 / 深链时页面本身在转场，鱼慢慢游会横穿新页面，
 * 于是改成穿越——消失的那一小段（50ms）天然掩盖了位置跳变。
 *
 * 时间轴（共 0.41s，均为纯函数、可单测）：
 * ```
 * 0.00 ──0.16──  0.21  ────0.41
 * 淡出    消失   换位置   淡入(带回弹)
 * ```
 */
internal object FishTeleport {

    /** 原地淡出时长：太短会像闪一下就没了，太长会显得迟钝。 */
    const val FADE_OUT_SECONDS = 0.16f

    /** 完全不可见的时长：换位置就发生在这里面的一瞬。 */
    const val GAP_SECONDS = 0.05f

    /** 目标点淡入时长：含 1.12 倍的回弹。 */
    const val FADE_IN_SECONDS = 0.20f

    /** 淡出时缩到多小，淡入时从这个值放大回来。 */
    private const val SHRINK_SCALE = 0.72f

    /** 淡入的回弹峰值：略大于 1 才有「啪一下出现」的弹性。 */
    private const val BOUNCE_SCALE = 1.12f

    /** 回弹峰值的归一化位置（0..1）。 */
    const val BOUNCE_AT = 0.6f

    /** 整段时长。 */
    const val TOTAL_SECONDS = FADE_OUT_SECONDS + GAP_SECONDS + FADE_IN_SECONDS

    /** 是否已到「换位置」的时刻（此帧之前必须完全不可见）。 */
    const val ARRIVE_AT_SECONDS = FADE_OUT_SECONDS + GAP_SECONDS

    /**
     * 整体不透明度。
     *
     * [elapsed] 为负表示不在穿越中，直接返回 1，调用方不必分支。
     */
    fun alpha(elapsed: Float): Float {
        if (elapsed < 0f) return 1f
        return when {
            elapsed < FADE_OUT_SECONDS -> 1f - elapsed / FADE_OUT_SECONDS
            elapsed < ARRIVE_AT_SECONDS -> 0f
            elapsed < TOTAL_SECONDS -> ((elapsed - ARRIVE_AT_SECONDS) / FADE_IN_SECONDS).coerceIn(0f, 1f)
            else -> 1f
        }
    }

    /**
     * 缩放：淡出时缩到 0.72（"吸走"），淡入时放大到 1.12 再落回 1（"弹出"）。
     *
     * 不用非等比缩放：穿越是位移不是挤压，等比才不会读成受惊/起跳。
     */
    fun scale(elapsed: Float): Float {
        if (elapsed < 0f) return 1f
        return when {
            elapsed < FADE_OUT_SECONDS -> {
                val t = elapsed / FADE_OUT_SECONDS
                1f - (1f - SHRINK_SCALE) * t
            }

            elapsed < ARRIVE_AT_SECONDS -> SHRINK_SCALE
            elapsed < TOTAL_SECONDS -> {
                val t = ((elapsed - ARRIVE_AT_SECONDS) / FADE_IN_SECONDS).coerceIn(0f, 1f)
                val peak = SHRINK_SCALE + (BOUNCE_SCALE - SHRINK_SCALE) * (t / BOUNCE_AT)
                if (t < BOUNCE_AT) peak else BOUNCE_SCALE + (1f - BOUNCE_SCALE) * ((t - BOUNCE_AT) / (1f - BOUNCE_AT))
            }

            else -> 1f
        }
    }

    /** 是否到了换位置的那一刻（调用方据此把坐标瞬移过去，并放一圈涟漪）。 */
    fun hasArrived(elapsed: Float): Boolean = elapsed >= ARRIVE_AT_SECONDS

    /** 是否整段结束。 */
    fun hasFinished(elapsed: Float): Boolean = elapsed >= TOTAL_SECONDS
}
