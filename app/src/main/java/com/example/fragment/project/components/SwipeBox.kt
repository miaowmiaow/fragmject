package com.example.fragment.project.components

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeBox(
    modifier: Modifier = Modifier,
    actionWidth: Dp,
    startAction: List<@Composable BoxScope.() -> Unit> = listOf(),
    startFillAction: (@Composable BoxScope.() -> Unit)? = null,
    endAction: List<@Composable BoxScope.() -> Unit> = listOf(),
    endFillAction: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) {
        actionWidth.toPx()
    }
    val startWidth = actionWidthPx * startAction.size
    val startActionSize = startAction.size + 1 // startAction + startFillAction
    val endWidth = actionWidthPx * endAction.size
    val endActionSize = endAction.size + 1 // endAction + endFillAction
    var contentWidth by remember { mutableFloatStateOf(0f) }
    var contentHeight by remember { mutableFloatStateOf(0f) }
    val state = remember(startWidth, endWidth, contentWidth) {
        AnchoredDraggableState(
            initialValue = DragAnchors.Center,
            animationSpec = TweenSpec(durationMillis = 350),
            anchors = DraggableAnchors {
                DragAnchors.Start at (if (startFillAction != null) actionWidthPx else 0f) + startWidth
                DragAnchors.StartFill at (if (startFillAction != null) contentWidth else 0f) + startWidth
                DragAnchors.Center at 0f
                DragAnchors.End at (if (endFillAction != null) -actionWidthPx else 0f) - endWidth
                DragAnchors.EndFill at (if (endFillAction != null) -contentWidth else 0f) - endWidth
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
        )
    }

    Box(
        modifier = modifier
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
            )
            .clipToBounds()
    ) {
        startAction.forEachIndexed { index, action ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(actionWidth)
                    .height(with(density) {
                        contentHeight.toDp()
                    })
                    .offset {
                        IntOffset(
                            x = if (state.offset <= actionWidthPx * startActionSize) {
                                (-actionWidthPx + state.offset / startActionSize * (startActionSize - index)).roundToInt()
                            } else {
                                (-actionWidthPx * (index + 1) + state.offset).roundToInt()
                            },
                            y = 0,
                        )
                    }
            ) {
                action()
            }
        }
        startFillAction?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(with(density) {
                        contentHeight.toDp()
                    })
                    .offset {
                        IntOffset(
                            x = if (state.offset <= actionWidthPx * startActionSize) {
                                (-contentWidth + state.offset / startActionSize).roundToInt()
                            } else {
                                (-contentWidth - startWidth + state.offset).roundToInt()
                            },
                            y = 0,
                        )
                    }
            ) {
                it()
            }
        }
        endAction.forEachIndexed { index, action ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth)
                    .height(with(density) {
                        contentHeight.toDp()
                    })
                    .offset {
                        IntOffset(
                            x = if (state.offset >= -(actionWidthPx * endActionSize)) {
                                (actionWidthPx + state.offset / endActionSize * (endActionSize - index)).roundToInt()
                            } else {
                                (actionWidthPx * (index + 1) + state.offset).roundToInt()
                            },
                            y = 0,
                        )
                    }
            ) {
                action()
            }
        }
        endFillAction?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(with(density) {
                        contentHeight.toDp()
                    })
                    .offset {
                        IntOffset(
                            x = if (state.offset >= -(actionWidthPx * endActionSize)) {
                                (contentWidth + state.offset / endActionSize).roundToInt()
                            } else {
                                (contentWidth + endWidth + state.offset).roundToInt()
                            },
                            y = 0,
                        )
                    }
            ) {
                it()
            }
        }
        Box(
            modifier = Modifier
                .onSizeChanged {
                    contentWidth = it.width.toFloat()
                    contentHeight = it.height.toFloat()
                }
                .offset {
                    IntOffset(
                        x = state.offset.roundToInt(),
                        y = 0,
                    )
                }
        ) {
            content()
        }
    }
}

enum class DragAnchors {
    Start,
    StartFill,
    Center,
    End,
    EndFill,
}