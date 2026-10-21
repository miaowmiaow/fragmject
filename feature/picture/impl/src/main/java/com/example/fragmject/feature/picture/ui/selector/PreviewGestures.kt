package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 预览页手势：双指缩放/平移、单指平移（已放大时）、单击切换上下栏、双击复位/放大。
 *
 * 单指且未放大时不消费事件，交给 HorizontalPager 翻页。
 */
internal fun Modifier.previewGestures(
    zoomState: PageZoomState,
    onToggleBars: () -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        awaitEachGesture {
            do {
                val event = awaitPointerEvent()
                val activePointers = event.changes.filter { it.pressed }

                if (activePointers.size >= 2) {
                    // 多指：计算缩放和平移
                    val p1 = activePointers[0]
                    val p2 = activePointers[1]
                    val prevCentroid = (p1.previousPosition + p2.previousPosition) / 2f
                    val currCentroid = (p1.position + p2.position) / 2f
                    val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()
                    val currDist = (p1.position - p2.position).getDistance()
                    val zoom = if (prevDist > 0f) currDist / prevDist else 1f

                    zoomState.scale = (zoomState.scale * zoom).coerceIn(0.3f, 5f)
                    zoomState.offsetX += (currCentroid - prevCentroid).x
                    zoomState.offsetY += (currCentroid - prevCentroid).y
                    activePointers.forEach { it.consume() }
                } else if (activePointers.size == 1 && zoomState.scale != 1f) {
                    // 单指 + 非原始尺寸：平移
                    val change = activePointers.first()
                    val delta: Offset = change.position - change.previousPosition
                    zoomState.offsetX += delta.x
                    zoomState.offsetY += delta.y
                    change.consume()
                }
                // 单指 + scale=1：不消费，让 HorizontalPager 处理翻页
            } while (event.changes.any { it.pressed })
        }
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { onToggleBars() },
            onDoubleTap = {
                if (zoomState.scale != 1f) {
                    zoomState.reset()
                } else {
                    zoomState.scale = 2.5f
                }
            }
        )
    }
