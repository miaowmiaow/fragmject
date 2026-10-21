package com.example.fragmject.core.image.editor.components

import android.view.MotionEvent
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import kotlin.math.sqrt

/**
 * 把 Compose 指针事件翻译成 [MotionEvent] 交给 [PictureEditorState]，并补齐手势补帧。
 *
 * 原先这些状态与分支内联在 `PictureEditorCanvas` 的 `pointerInput` 里，使该组合函数
 * 圈复杂度长期超阈值；抽成普通类后，画布侧只剩「取事件 → 转交」的循环。
 */
@Stable
internal class PictureEditorGestureHandler(
    private val state: PictureEditorState,
) {

    private var downTime = 0L
    private var wasPressed = false
    private var prevCentroid = Offset.Zero
    private var prevSpan = 0f
    private var prevCount = 0

    /** 处理一帧指针事件：按下 / 抬起 / 移动 / 多指增删。 */
    fun onEvent(changes: List<PointerInputChange>) {
        val first = changes.firstOrNull() ?: return
        val currentTime = System.currentTimeMillis()
        val allPressed = changes.all { it.pressed }
        val anyPressed = changes.any { it.pressed }
        val centroid = centroidOf(changes, first)
        val span = spanOf(changes)

        val action = detectAction(currentTime, allPressed, anyPressed, changes.size)
        if (action >= 0) {
            val handled = dispatch(action, first, currentTime)
            val gestureStillActive = wasPressed && allPressed
            val pointerCountUnchanged = changes.size == prevCount
            if (!handled && gestureStillActive && pointerCountUnchanged) {
                handleTransform(centroid, span, changes.size)
            }
            if (action == MotionEvent.ACTION_DOWN) {
                state.handleDoubleTap(first.position.x, first.position.y)
            }
        }

        wasPressed = anyPressed
        prevCentroid = if (anyPressed) centroid else Offset.Zero
        prevSpan = if (anyPressed && span > 0) span else 0f
        prevCount = changes.size

        changes.forEach { it.consume() }
    }

    /** 依据按下状态与手指数变化推断等价 [MotionEvent] action；无法判定时返回 -1。 */
    private fun detectAction(
        currentTime: Long,
        allPressed: Boolean,
        anyPressed: Boolean,
        count: Int,
    ): Int = when {
        !wasPressed && allPressed -> {
            downTime = currentTime
            MotionEvent.ACTION_DOWN
        }
        wasPressed && !anyPressed -> MotionEvent.ACTION_UP
        wasPressed && allPressed && count != prevCount ->
            if (count > prevCount) MotionEvent.ACTION_POINTER_DOWN else MotionEvent.ACTION_POINTER_UP
        wasPressed && allPressed -> MotionEvent.ACTION_MOVE
        else -> -1
    }

    /** 把单个 action 包成 [MotionEvent] 交给编辑器；返回编辑器是否已消费。 */
    private fun dispatch(
        action: Int,
        first: PointerInputChange,
        currentTime: Long,
    ): Boolean {
        val me = MotionEvent.obtain(
            downTime, currentTime, action,
            first.position.x, first.position.y, 0
        )
        return try {
            state.handleTouchEvent(me)
        } finally {
            me.recycle()
        }
    }

    /** 编辑器未消费时按质心位移滚动、按双指距离缩放。 */
    private fun handleTransform(centroid: Offset, span: Float, pointerCount: Int) {
        if (prevCentroid != Offset.Zero && centroid != Offset.Zero) {
            val dx = centroid.x - prevCentroid.x
            val dy = centroid.y - prevCentroid.y
            if (pointerCount == 1) {
                state.handleScroll(dx, dy)
            }
        }
        if (span > 0 && prevSpan > 0 && pointerCount >= 2) {
            state.handleScale(span / prevSpan, centroid.x, centroid.y)
        }
    }
}

/** 多指时取所有触点质心，单指时用当前触点位置。 */
private fun centroidOf(changes: List<PointerInputChange>, fallback: PointerInputChange): Offset =
    if (changes.size >= 2) {
        val positions = changes.map { it.position }
        Offset(
            positions.sumOf { it.x.toDouble() }.toFloat() / changes.size,
            positions.sumOf { it.y.toDouble() }.toFloat() / changes.size
        )
    } else {
        fallback.position
    }

/** 双指距离；单指时为 0（不参与缩放）。 */
private fun spanOf(changes: List<PointerInputChange>): Float =
    if (changes.size >= 2) {
        val p0 = changes[0].position
        val p1 = changes[1].position
        val dx = p1.x - p0.x
        val dy = p1.y - p0.y
        sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    } else {
        0f
    }
