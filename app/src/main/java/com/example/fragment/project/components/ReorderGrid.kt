package com.example.fragment.project.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ReorderLazyVerticalGrid(
    items: List<T>,
    key: ((index: Int, item: T) -> Any),
    onMove: (from: Int, to: Int) -> Unit,
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    itemContent: @Composable BoxScope.(index: Int, item: T) -> Unit
) {
    val scope = rememberCoroutineScope()
    val autoScrollThreshold = with(LocalDensity.current) { 50.dp.toPx() }
    val layoutInfo by remember { derivedStateOf { state.layoutInfo } }
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var draggingItemDelta by remember { mutableStateOf(Offset.Zero) }

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    draggingItemIndex = layoutInfo.firstOrNull(offset)?.index ?: -1
                },
                onDragEnd = {
                    draggingItemIndex = -1
                    draggingItemDelta = Offset.Zero
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    draggingItemDelta += dragAmount
                    val targetItem = layoutInfo.firstOrNull(change.position)
                        ?: return@detectDragGesturesAfterLongPress
                    val targetItemIndex = targetItem.index
                    val targetItemOffset = targetItem.offset.toOffset()
                    val targetItemCenter = targetItem.size.toOffset() * 0.5f
                    scope.launch {
                        draggingItemDelta = change.position - targetItemOffset - targetItemCenter
                        if (draggingItemIndex != -1 && draggingItemIndex != targetItemIndex) {
                            when {
                                targetItemIndex == state.firstVisibleItemIndex -> draggingItemIndex
                                draggingItemIndex == state.firstVisibleItemIndex -> targetItemIndex
                                else -> null
                            }?.let {
                                // this is needed to neutralize automatic keeping the first item first.
                                // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/androidx/compose/foundation/demos/LazyGridDragAndDropDemo.kt
                                state.scrollToItem(it, state.firstVisibleItemScrollOffset)
                            }
                            onMove(draggingItemIndex, targetItemIndex)
                            draggingItemIndex = targetItemIndex
                        } else {
                            val distFromTop = change.position.y
                            if (distFromTop < autoScrollThreshold) {
                                state.scrollBy(-(autoScrollThreshold - distFromTop))
                            }
                            val distFromBottom =
                                layoutInfo.viewportEndOffset - change.position.y
                            if (distFromBottom < autoScrollThreshold) {
                                state.scrollBy(autoScrollThreshold - distFromBottom)
                            }
                            delay(50)
                        }
                    }
                }
            )
        },
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled
    ) {
        itemsIndexed(items, key) { index, item ->
            Box(modifier = Modifier
                .scale(
                    updateTransition(draggingItemIndex == index, label = "selected")
                        .animateFloat(label = "scale") { selected ->
                            if (selected) 0.9f else 1f
                        }.value
                )
                .then(
                    if (draggingItemIndex == index) {
                        Modifier
                            .offset {
                                IntOffset(
                                    draggingItemDelta.x.roundToInt(),
                                    draggingItemDelta.y.roundToInt()
                                )
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

fun IntSize.toOffset() = IntOffset(width, height).toOffset()

fun IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())

fun LazyGridLayoutInfo.firstOrNull(hitPoint: Offset): LazyGridItemInfo? =
    visibleItemsInfo.firstOrNull() { item ->
        hitPoint.x.toInt() in item.offset.x..item.offset.x + item.size.width &&
                hitPoint.y.toInt() in item.offset.y..item.offset.y + item.size.height
    }