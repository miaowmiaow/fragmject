package com.example.fragment.project.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * 自定义下拉刷新&加载更多
 * @param items      列表数据
 * @param refreshing 设置下拉刷新
 * @param loading    设置加载更多
 * @param finishing  结束加载更多
 * @param onRefresh  下拉刷新回调
 * @param onLoad     加载更多回调
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> SwipeRefresh(
    items: List<T>?,
    refreshing: Boolean,
    loading: Boolean,
    finishing: Boolean,
    onRefresh: () -> Unit,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    if (items.isNullOrEmpty()) {
        if (!finishing) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(id = R.color.theme_orange)
                )
            }
        } else {
            EmptyContent {
                onRefresh()
            }
        }
    } else {
        val state = rememberSwipeRefreshState(refreshing, onRefresh)
        Box(
            modifier = Modifier
                .swipeRefresh(state)
                .clipToBounds()
                .background(colorResource(R.color.background_refresh))
                .graphicsLayer {
                    translationY = state.position
                },
            contentAlignment = Alignment.TopCenter
        ) {
            RefreshIndicator(refreshing) { state.position }
            Box(
                modifier = Modifier.background(colorResource(R.color.background))
            ) {
                LazyColumn(
                    modifier = modifier,
                    state = listState,
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                ) {
                    itemsIndexed(
                        items = items,
                        key = key,
                        contentType = contentType
                    ) { index, item ->
                        itemContent(index, item)
                        if (loading && items.size - index < 5) {
                            LaunchedEffect(items.size) { onLoad() }
                        }
                    }
                    item { MoreIndicator(finishing) }
                }
            }
        }
    }
}

@Composable
fun MoreIndicator(
    finishing: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = if (finishing) "没有更多了！" else "正在加载中...",
            fontSize = 12.sp,
            color = colorResource(R.color.theme),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RefreshIndicator(
    refreshing: Boolean,
    position: () -> Float
) {
    val refreshingResId = listOf(
        R.mipmap.refreshing_1,
        R.mipmap.refreshing_2,
        R.mipmap.refreshing_3,
        R.mipmap.refreshing_4,
        R.mipmap.refreshing_5,
        R.mipmap.refreshing_6,
        R.mipmap.refreshing_7,
        R.mipmap.refreshing_8,
        R.mipmap.refreshing_9,
        R.mipmap.refreshing_10,
        R.mipmap.refreshing_11,
        R.mipmap.refreshing_12,
        R.mipmap.refreshing_13,
        R.mipmap.refreshing_14,
        R.mipmap.refreshing_15,
        R.mipmap.refreshing_16,
        R.mipmap.refreshing_17,
        R.mipmap.refreshing_18,
        R.mipmap.refreshing_19,
        R.mipmap.refreshing_20,
        R.mipmap.refreshing_21,
        R.mipmap.refreshing_22,
        R.mipmap.refreshing_23,
        R.mipmap.refreshing_24,
        R.mipmap.refreshing_25,
        R.mipmap.refreshing_26,
        R.mipmap.refreshing_27,
        R.mipmap.refreshing_28,
        R.mipmap.refreshing_29,
        R.mipmap.refreshing_30,
        R.mipmap.refreshing_31,
        R.mipmap.refreshing_32,
        R.mipmap.refreshing_33,
        R.mipmap.refreshing_34,
        R.mipmap.refreshing_35,
        R.mipmap.refreshing_36,
        R.mipmap.refreshing_37,
    )
    val loadingHeight = SwipeRefreshDefaults.RefreshThreshold
    val loadingHeightPx: Float
    with(LocalDensity.current) {
        loadingHeightPx = loadingHeight.toPx()
    }
    val infiniteTransition = rememberInfiniteTransition(label = "SwipeRefresh")
    val loadingAnimate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = refreshingResId.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingAnimate"
    )
    val id = if (refreshing) loadingAnimate else position() % refreshingResId.size
    Image(
        painter = painterResource(refreshingResId[id.toInt()]),
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer {
                translationY = (-position() - loadingHeightPx) * 0.5f
            }
            .size(loadingHeight, loadingHeight),
        contentScale = ContentScale.Crop,
    )
}

@Composable
@ExperimentalMaterialApi
fun rememberSwipeRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    refreshThreshold: Dp = SwipeRefreshDefaults.RefreshThreshold,
    refreshingOffset: Dp = SwipeRefreshDefaults.RefreshingOffset,
): SwipeRefreshState {
    require(refreshThreshold > 0.dp) { "The refresh trigger must be greater than zero!" }

    val scope = rememberCoroutineScope()
    val onRefreshState = rememberUpdatedState(onRefresh)
    val thresholdPx: Float
    val refreshingOffsetPx: Float

    with(LocalDensity.current) {
        thresholdPx = refreshThreshold.toPx()
        refreshingOffsetPx = refreshingOffset.toPx()
    }

    val state = remember(scope) {
        SwipeRefreshState(scope, onRefreshState, refreshingOffsetPx, thresholdPx)
    }

    SideEffect {
        state.setRefreshing(refreshing)
        state.setThreshold(thresholdPx)
        state.setRefreshingOffset(refreshingOffsetPx)
    }

    return state
}

@ExperimentalMaterialApi
fun Modifier.swipeRefresh(
    state: SwipeRefreshState,
    enabled: Boolean = true
) = inspectable(inspectorInfo = debugInspectorInfo {
    name = "pullRefresh"
    properties["state"] = state
    properties["enabled"] = enabled
}) {
    Modifier.pullRefresh(state::onPull, state::onRelease, enabled)
}

@ExperimentalMaterialApi
class SwipeRefreshState internal constructor(
    private val animationScope: CoroutineScope,
    private val onRefreshState: State<() -> Unit>,
    refreshingOffset: Float,
    threshold: Float
) {

    private val progress get() = adjustedDistancePulled / threshold

    internal val refreshing get() = _refreshing
    val position get() = _position
    private val threshold get() = _threshold

    private val adjustedDistancePulled by derivedStateOf { distancePulled * 0.5f }

    private var _refreshing by mutableStateOf(false)
    private var _position by mutableFloatStateOf(0f)
    private var distancePulled by mutableFloatStateOf(0f)
    private var _threshold by mutableFloatStateOf(threshold)
    private var _refreshingOffset by mutableFloatStateOf(refreshingOffset)

    internal fun onPull(pullDelta: Float): Float {
        if (_refreshing) return 0f // Already refreshing, do nothing.

        val newOffset = (distancePulled + pullDelta).coerceAtLeast(0f)
        val dragConsumed = newOffset - distancePulled
        distancePulled = newOffset
        _position = calculateIndicatorPosition()
        return dragConsumed
    }

    internal fun onRelease(velocity: Float): Float {
        if (refreshing) return 0f // Already refreshing, do nothing

        if (adjustedDistancePulled > threshold) {
            onRefreshState.value()
        }
        animateIndicatorTo(0f)
        val consumed = when {
            // We are flinging without having dragged the pull refresh (for example a fling inside
            // a list) - don't consume
            distancePulled == 0f -> 0f
            // If the velocity is negative, the fling is upwards, and we don't want to prevent the
            // the list from scrolling
            velocity < 0f -> 0f
            // We are showing the indicator, and the fling is downwards - consume everything
            else -> velocity
        }
        distancePulled = 0f
        return consumed
    }

    internal fun setRefreshing(refreshing: Boolean) {
        if (_refreshing != refreshing) {
            _refreshing = refreshing
            distancePulled = 0f
            animateIndicatorTo(if (refreshing) _refreshingOffset else 0f)
        }
    }

    internal fun setThreshold(threshold: Float) {
        _threshold = threshold
    }

    internal fun setRefreshingOffset(refreshingOffset: Float) {
        if (_refreshingOffset != refreshingOffset) {
            _refreshingOffset = refreshingOffset
            if (refreshing) animateIndicatorTo(refreshingOffset)
        }
    }

    // Make sure to cancel any existing animations when we launch a new one. We use this instead of
    // Animatable as calling snapTo() on every drag delta has a one frame delay, and some extra
    // overhead of running through the animation pipeline instead of directly mutating the state.
    private val mutatorMutex = MutatorMutex()

    private fun animateIndicatorTo(offset: Float) = animationScope.launch {
        mutatorMutex.mutate {
            animate(initialValue = _position, targetValue = offset) { value, _ ->
                _position = value
            }
        }
    }

    private fun calculateIndicatorPosition(): Float = when {
        // If drag hasn't gone past the threshold, the position is the adjustedDistancePulled.
        adjustedDistancePulled <= threshold -> adjustedDistancePulled
        else -> {
            // How far beyond the threshold pull has gone, as a percentage of the threshold.
            val overshootPercent = abs(progress) - 1.0f
            // Limit the overshoot to 200%. Linear between 0 and 200.
            val linearTension = overshootPercent.coerceIn(0f, 2f)
            // Non-linear tension. Increases with linearTension, but at a decreasing rate.
            val tensionPercent = linearTension - linearTension.pow(2) / 4
            // The additional offset beyond the threshold.
            val extraOffset = threshold * tensionPercent
            threshold + extraOffset
        }
    }
}

@ExperimentalMaterialApi
object SwipeRefreshDefaults {
    /**
     * If the indicator is below this threshold offset when it is released, a refresh
     * will be triggered.
     */
    val RefreshThreshold = 100.dp

    /**
     * The offset at which the indicator should be rendered whilst a refresh is occurring.
     */
    val RefreshingOffset = 100.dp
}
