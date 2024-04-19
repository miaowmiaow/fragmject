package com.example.fragment.project.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
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
    val scope = rememberCoroutineScope()
    val layoutInfo by remember { derivedStateOf { state.layoutInfo } }
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    val draggingItemDelta by remember { mutableStateOf(Animatable(0f)) }
    val autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() }

    LazyColumn(
        modifier = modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    draggingItemIndex = layoutInfo.firstOrNull(offset)?.index ?: -1
                },
                onDragEnd = {
                    scope.launch {
                        draggingItemDelta.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = 1f
                            )
                        ) {
                            if (value == targetValue) {
                                draggingItemIndex = -1
                            }
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val targetItem = layoutInfo.firstOrNull(change.position)
                        ?: return@detectDragGesturesAfterLongPress
                    val targetItemIndex = targetItem.index
                    scope.launch {
                        draggingItemDelta.snapTo(draggingItemDelta.value + dragAmount.y)

                        val distFromTop = change.position.y
                        val distFromBottom = layoutInfo.viewportSize.height - change.position.y
                        when {
                            distFromTop < autoScrollThreshold -> distFromTop - autoScrollThreshold
                            distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                            else -> null
                        }?.let {
                            if(state.scrollBy(it) != 0f){
                                draggingItemDelta.snapTo(draggingItemDelta.value + it)
                                delay(10)
                            }
                        }

                        if (draggingItemIndex != -1 && draggingItemIndex != targetItemIndex) {
                            when {
                                targetItemIndex == state.firstVisibleItemIndex -> draggingItemIndex
                                draggingItemIndex == state.firstVisibleItemIndex -> targetItemIndex
                                else -> null
                            }?.let {
                                // this is needed to neutralize automatic keeping the first item first.
                                state.scrollToItem(it, state.firstVisibleItemScrollOffset)
                            }
                            onMove(draggingItemIndex, targetItemIndex)
                            draggingItemIndex = targetItemIndex
                            draggingItemDelta.snapTo(change.position.y - targetItem.offset - targetItem.size * 0.5f)
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
                    if (draggingItemIndex == index) {
                        Modifier
                            .offset {
                                IntOffset(
                                    0,
                                    draggingItemDelta.value.roundToInt()
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

fun LazyListLayoutInfo.firstOrNull(hitPoint: Offset): LazyListItemInfo? =
    visibleItemsInfo.firstOrNull { item ->
        hitPoint.y.toInt() in item.offset..(item.offset + item.size)
    }