package com.example.fragment.project.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshDefaults
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.fragment.library.base.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * 自定义下拉刷新&加载更多
 * @param refreshing 设置下拉刷新状态
 * @param loading    设置加载更多状态
 * @param onRefresh  下拉刷新回调
 * @param onLoad     加载更多回调
 * @param onRetry    重试回调
 * @param data       列表数据
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun <T> SwipeRefresh(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    refreshing: Boolean,
    loading: Boolean,
    onRefresh: () -> Unit,
    onLoad: () -> Unit,
    onRetry: () -> Unit = {},
    data: List<T>?,
    contentTypeMapping: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    val loadingResId = listOf(
        R.drawable.loading_big_1,
        R.drawable.loading_big_4,
        R.drawable.loading_big_7,
        R.drawable.loading_big_10,
        R.drawable.loading_big_13,
        R.drawable.loading_big_16,
        R.drawable.loading_big_19,
    )
    val loadingHeightPx: Float
    with(LocalDensity.current) {
        loadingHeightPx = 16.dp.toPx()
    }
    val loadingAnimate by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = loadingResId.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val state = rememberSwipeRefreshState(refreshing, onRefresh)
    if (data.isNullOrEmpty()) {
        if (!refreshing) {
            Empty { onRetry() }
        }
    } else {
        Box(Modifier.swipeRefresh(state)) {
            LazyColumn(
                modifier = modifier.graphicsLayer {
                    translationY = state.position
                },
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
            ) {
                itemsIndexed(data, contentType = contentTypeMapping) { index, item ->
                    itemContent(index, item)
                    if (loading && data.size - index < 5) {
                        LaunchedEffect(data.size) {
                            onLoad()
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                onLoad()
                            }
                    ) {
                        Text(
                            text = "👆👆👇👇👈👉👈👉🅱🅰🅱🅰",
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.gray),
                            modifier = Modifier.align(alignment = Alignment.Center)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = (refreshing || (state.position >= loadingHeightPx * 0.5f)),
                modifier = Modifier
                    .size(40.dp, 16.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = state.position * 0.5f
                    },
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                val id = if (refreshing) loadingAnimate else state.position % loadingResId.size
                Image(
                    painter = painterResource(loadingResId[id.toInt()]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
@ExperimentalMaterialApi
fun rememberSwipeRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
    refreshingOffset: Dp = PullRefreshDefaults.RefreshingOffset,
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

    val progress get() = adjustedDistancePulled / threshold

    internal val refreshing get() = _refreshing
    val position get() = _position
    internal val threshold get() = _threshold

    private val adjustedDistancePulled by derivedStateOf { distancePulled * 0.5f }

    private var _refreshing by mutableStateOf(false)
    private var _position by mutableStateOf(0f)
    private var distancePulled by mutableStateOf(0f)
    private var _threshold by mutableStateOf(threshold)
    private var _refreshingOffset by mutableStateOf(refreshingOffset)

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
