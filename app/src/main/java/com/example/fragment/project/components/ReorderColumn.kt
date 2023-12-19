package com.example.fragment.project.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ReorderLazyColumn(
    items: List<T>,
    key: ((index: Int, item: T) -> Any),
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    itemContent: @Composable BoxScope.(index: Int, item: T) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val layoutInfo by remember { derivedStateOf { state.layoutInfo } }
    var pressedKey by remember { mutableIntStateOf(-1) }
    var offsetY by remember { mutableStateOf(0f) }
    val autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() }

    LazyColumn(
        modifier = modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    layoutInfo.gridItemAtPosition(offset)?.let { info ->
                        pressedKey = info.index
                    }
                },
                onDragEnd = {
                    offsetY = 0f
                    pressedKey = -1
                },
                onDragCancel = {
                    pressedKey = -1
                },
                onDrag = { change, offset ->
                    change.consume()
                    offsetY += offset.y
                    layoutInfo.gridItemAtPosition(change.position)?.let { info ->
                        val nextKey = info.index
                        if (pressedKey != -1 && pressedKey != nextKey) {
                            onMove(pressedKey, nextKey)
                            offsetY = change.position.y - info.offset - info.size * 0.5f
                            pressedKey = nextKey
                        }
                        coroutineScope.launch {
                            val distFromBottom =
                                state.layoutInfo.viewportSize.height - change.position.y
                            val distFromTop = change.position.y
                            state.scrollBy(
                                when {
                                    distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                                    distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                                    else -> 0f
                                }
                            )
                        }
                    }
                }
            )
        },
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled
    ) {
        itemsIndexed(items, key) { index, item ->
            Box(modifier = Modifier
                .then(
                    if (pressedKey == index) {
                        Modifier
                            .offset {
                                IntOffset(0, offsetY.roundToInt())
                            }
                            .zIndex(1f)
                            .shadow(8.dp)
                    } else {
                        Modifier
                            .zIndex(0f)
                            .shadow(0.dp)
                            .animateItemPlacement()
                    }
                )) {
                itemContent(index, item)
            }
        }
    }
}

fun LazyListLayoutInfo.gridItemAtPosition(hitPoint: Offset): LazyListItemInfo? =
    visibleItemsInfo.find { itemInfo ->
        IntRect(
            left = 0,
            top = itemInfo.offset,
            right = viewportSize.width,
            bottom = itemInfo.offset + itemInfo.size
        ).contains(hitPoint.round())
    }