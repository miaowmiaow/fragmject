package com.example.fragment.project.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    data: List<T>,
    visibleCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int, item: T) -> Unit,
) {
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val density = LocalDensity.current
        val count = data.size
        val pickerHeight = maxHeight
        val pickerHeightPx = density.run { pickerHeight.toPx() }
        val pickerCenterLinePx = pickerHeightPx / 2
        val itemHeight = pickerHeight / visibleCount
        val itemHeightPx = pickerHeightPx / visibleCount
        val startIndex = Int.MAX_VALUE / 2
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = startIndex - startIndex % count,
            initialFirstVisibleItemScrollOffset = ((itemHeightPx - pickerHeightPx) / 2).roundToInt(),
        )
        val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }
        LazyColumn(
            modifier = Modifier,
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
        ) {
            items(Int.MAX_VALUE) { index ->
                val item = layoutInfo.visibleItemsInfo.find { it.index == index }
                var currentsAdjust = 1f
                if (item != null) {
                    val itemCenterY = item.offset + item.size / 2
                    currentsAdjust = 0.75f + 0.25f * if (itemCenterY < pickerCenterLinePx) {
                        itemCenterY / pickerCenterLinePx
                    } else {
                        1 - (itemCenterY - pickerCenterLinePx) / pickerCenterLinePx
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            alpha = currentsAdjust
                            scaleX = currentsAdjust
                            scaleY = currentsAdjust
                            rotationX = (1 + currentsAdjust) * 180
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val currIndex = (index - startIndex).floorMod(count)
                    content(currIndex, data[currIndex])
                }
            }
        }
    }
}

private fun Int.floorMod(other: Int): Int = when (other) {
    0 -> this
    else -> this - floorDiv(other) * other
}

