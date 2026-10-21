package com.example.fragmject.core.ui.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SwipeBox(
    actionWidth: Dp,
    modifier: Modifier = Modifier,
    control: SwipeBoxControl = rememberSwipeBoxControl(),
    startAction: List<@Composable BoxScope.() -> Unit> = listOf(),
    startFillAction: (@Composable BoxScope.() -> Unit)? = null,
    endAction: List<@Composable BoxScope.() -> Unit> = listOf(),
    endFillAction: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val startWidth = actionWidthPx * startAction.size
    val endWidth = actionWidthPx * endAction.size
    // 槽位总数 = 普通动作数 + 填充动作（若存在）
    val startActionSize = startAction.size + if (startFillAction == null) 0 else 1
    val endActionSize = endAction.size + if (endFillAction == null) 0 else 1
    var contentWidth by remember { mutableFloatStateOf(0f) }
    var contentHeight by remember { mutableFloatStateOf(0f) }
    val contentHeightDp = with(density) { contentHeight.toDp() }
    val state = remember(startWidth, endWidth, contentWidth) {
        AnchoredDraggableState(
            initialValue = DragAnchors.Center,
            anchors = swipeBoxAnchors(
                actionWidthPx = actionWidthPx,
                startWidth = startWidth,
                endWidth = endWidth,
                contentWidth = contentWidth,
                startFillEnabled = startFillAction != null,
                endFillEnabled = endFillAction != null,
            ),
        )
    }
    LaunchedEffect(control, state) {
        bindSwipeBoxControl(control = control, state = state, scope = scope)
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
            SwipeActionSlot(
                alignment = Alignment.CenterStart,
                width = actionWidth,
                height = contentHeightDp,
                offsetX = { startActionOffsetX(index, actionWidthPx, startActionSize, state.offset) },
            ) { action() }
        }
        startFillAction?.let { fill ->
            SwipeActionSlot(
                alignment = Alignment.CenterStart,
                width = null,
                height = contentHeightDp,
                offsetX = { startFillOffsetX(contentWidth, startWidth, actionWidthPx, startActionSize, state.offset) },
            ) { fill() }
        }
        endAction.forEachIndexed { index, action ->
            SwipeActionSlot(
                alignment = Alignment.CenterEnd,
                width = actionWidth,
                height = contentHeightDp,
                offsetX = { endActionOffsetX(index, actionWidthPx, endActionSize, state.offset) },
            ) { action() }
        }
        endFillAction?.let { fill ->
            SwipeActionSlot(
                alignment = Alignment.CenterEnd,
                width = null,
                height = contentHeightDp,
                offsetX = { endFillOffsetX(contentWidth, endWidth, actionWidthPx, endActionSize, state.offset) },
            ) { fill() }
        }
        SwipeContent(
            offsetX = { state.offset.roundToInt() },
            onSizeChanged = { width, height ->
                contentWidth = width
                contentHeight = height
            },
        ) { content() }
    }
}

/** 单个动作槽位：按拖动偏移摆到左右两侧，宽度为 null 表示填充式（跟随内容宽度）。 */
@Composable
private fun BoxScope.SwipeActionSlot(
    alignment: Alignment,
    width: Dp?,
    height: Dp,
    offsetX: () -> Int,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .offset { IntOffset(x = offsetX(), y = 0) }
    ) {
        content()
    }
}

/** 内容层：跟随拖拽偏移，并把自身尺寸回传给父级计算锚点。 */
@Composable
private fun SwipeContent(
    offsetX: () -> Int,
    onSizeChanged: (width: Float, height: Float) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .onSizeChanged { onSizeChanged(it.width.toFloat(), it.height.toFloat()) }
            .offset { IntOffset(x = offsetX(), y = 0) }
    ) {
        content()
    }
}

/** 拖拽锚点：内容两侧分别留出动作区高度（填充式动作额外留出一格与整屏宽度）。 */
private fun swipeBoxAnchors(
    actionWidthPx: Float,
    startWidth: Float,
    endWidth: Float,
    contentWidth: Float,
    startFillEnabled: Boolean,
    endFillEnabled: Boolean,
): DraggableAnchors<DragAnchors> = DraggableAnchors {
    DragAnchors.Start at (if (startFillEnabled) actionWidthPx else 0f) + startWidth
    DragAnchors.StartFill at (if (startFillEnabled) contentWidth else 0f) + startWidth
    DragAnchors.Center at 0f
    DragAnchors.End at (if (endFillEnabled) -actionWidthPx else 0f) - endWidth
    DragAnchors.EndFill at (if (endFillEnabled) -contentWidth else 0f) - endWidth
}

/** 左侧第 [index] 个动作的横向位移。 */
private fun startActionOffsetX(
    index: Int,
    actionWidthPx: Float,
    startActionSize: Int,
    offset: Float,
): Int = if (offset <= actionWidthPx * startActionSize) {
    (-actionWidthPx + offset / startActionSize * (startActionSize - index)).roundToInt()
} else {
    (-actionWidthPx * (index + 1) + offset).roundToInt()
}

/** 左侧填充动作的横向位移。 */
private fun startFillOffsetX(
    contentWidth: Float,
    startWidth: Float,
    actionWidthPx: Float,
    startActionSize: Int,
    offset: Float,
): Int = if (offset <= actionWidthPx * startActionSize) {
    (-contentWidth + offset / startActionSize).roundToInt()
} else {
    (-contentWidth - startWidth + offset).roundToInt()
}

/** 右侧第 [index] 个动作的横向位移。 */
private fun endActionOffsetX(
    index: Int,
    actionWidthPx: Float,
    endActionSize: Int,
    offset: Float,
): Int = if (offset >= -(actionWidthPx * endActionSize)) {
    (actionWidthPx + offset / endActionSize * (endActionSize - index)).roundToInt()
} else {
    (actionWidthPx * (index + 1) + offset).roundToInt()
}

/** 右侧填充动作的横向位移。 */
private fun endFillOffsetX(
    contentWidth: Float,
    endWidth: Float,
    actionWidthPx: Float,
    endActionSize: Int,
    offset: Float,
): Int = if (offset >= -(actionWidthPx * endActionSize)) {
    (contentWidth + offset / endActionSize).roundToInt()
} else {
    (contentWidth + endWidth + offset).roundToInt()
}

/** 把控制器的五个动作接到锚点动画上。 */
private suspend fun bindSwipeBoxControl(
    control: SwipeBoxControl,
    state: AnchoredDraggableState<DragAnchors>,
    scope: CoroutineScope,
) {
    with(control) {
        handleControlEvents(
            onStart = { scope.launch { state.animateTo(DragAnchors.Start) } },
            onStartFill = { scope.launch { state.animateTo(DragAnchors.StartFill) } },
            onCenter = { scope.launch { state.animateTo(DragAnchors.Center) } },
            onEnd = { scope.launch { state.animateTo(DragAnchors.End) } },
            onEndFill = { scope.launch { state.animateTo(DragAnchors.EndFill) } },
        )
    }
}

@Stable
class SwipeBoxControl(
    private val scope: CoroutineScope
) {
    private sealed interface ControlEvent {
        data object Start : ControlEvent
        data object StartFill : ControlEvent
        data object Center : ControlEvent
        data object End : ControlEvent
        data object EndFill : ControlEvent
    }

    private val controlEvents: MutableSharedFlow<ControlEvent> = MutableSharedFlow()

    @OptIn(FlowPreview::class)
    internal suspend fun handleControlEvents(
        onStart: () -> Unit = {},
        onStartFill: () -> Unit = {},
        onCenter: () -> Unit = {},
        onEnd: () -> Unit = {},
        onEndFill: () -> Unit = {},
    ) = withContext(Dispatchers.Main) {
        controlEvents.debounce(350.milliseconds).collect { event ->
            when (event) {
                ControlEvent.Start -> onStart()
                ControlEvent.StartFill -> onStartFill()
                ControlEvent.Center -> onCenter()
                ControlEvent.End -> onEnd()
                ControlEvent.EndFill -> onEndFill()
            }
        }
    }

    fun start() {
        scope.launch { controlEvents.emit(ControlEvent.Start) }
    }

    fun startFill() {
        scope.launch { controlEvents.emit(ControlEvent.StartFill) }
    }

    fun center() {
        scope.launch { controlEvents.emit(ControlEvent.Center) }
    }

    fun end() {
        scope.launch { controlEvents.emit(ControlEvent.End) }
    }

    fun endFill() {
        scope.launch { controlEvents.emit(ControlEvent.EndFill) }
    }
}

@Composable
fun rememberSwipeBoxControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): SwipeBoxControl = remember(coroutineScope) { SwipeBoxControl(coroutineScope) }

enum class DragAnchors {
    Start,
    StartFill,
    Center,
    End,
    EndFill,
}
