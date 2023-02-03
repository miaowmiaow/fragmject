package com.example.fragment.library.base.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.library.base.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun <T> PullRefreshLayout(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    loading: Boolean,
    onLoad: () -> Unit,
    onNoData: () -> Unit = {},
    items: List<T>,
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

    val state = rememberPullRefreshLayoutState(refreshing, onRefresh)
    if (items.isEmpty() && !refreshing) {
        NotNetworkLayout {
            onNoData()
        }
    } else {
        Box(Modifier.pullRefreshLayout(state)) {
            LazyColumn(
                modifier = modifier.graphicsLayer {
                    translationY = state.position
                },
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
            ) {
                itemsIndexed(items) { index, item ->
                    itemContent(index, item)
                    if (loading && items.size - index < 5) {
                        LaunchedEffect(items.size) {
                            onLoad()
                        }
                    }
                }
                if (items.isNotEmpty()) {
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
                                color = Color.Gray,
                                modifier = Modifier.align(alignment = Alignment.Center)
                            )
                        }
                    }
                }
            }
            // Custom progress indicator
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
fun rememberPullRefreshLayoutState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
    refreshingOffset: Dp = PullRefreshDefaults.RefreshingOffset,
): PullRefreshLayoutState {
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
        PullRefreshLayoutState(scope, onRefreshState, refreshingOffsetPx, thresholdPx)
    }

    SideEffect {
        state.setRefreshing(refreshing)
    }

    return state
}

@ExperimentalMaterialApi
fun Modifier.pullRefreshLayout(
    state: PullRefreshLayoutState,
    enabled: Boolean = true
) = inspectable(inspectorInfo = debugInspectorInfo {
    name = "pullRefresh"
    properties["state"] = state
    properties["enabled"] = enabled
}) {
    Modifier.pullRefresh(state::onPull, { state.onRelease() }, enabled)
}

@ExperimentalMaterialApi
class PullRefreshLayoutState internal constructor(
    private val animationScope: CoroutineScope,
    private val onRefreshState: State<() -> Unit>,
    private val refreshingOffset: Float,
    private val threshold: Float
) {

    val progress get() = adjustedDistancePulled / threshold

    internal val refreshing get() = _refreshing
    val position get() = _position

    private val adjustedDistancePulled by derivedStateOf { distancePulled * 0.5f }

    private var _refreshing by mutableStateOf(false)
    private var _position by mutableStateOf(0f)
    private var distancePulled by mutableStateOf(0f)

    internal fun onPull(pullDelta: Float): Float {
        if (this._refreshing) return 0f // Already refreshing, do nothing.

        val newOffset = (distancePulled + pullDelta).coerceAtLeast(0f)
        val dragConsumed = newOffset - distancePulled
        distancePulled = newOffset
        _position = calculateIndicatorPosition()
        return dragConsumed
    }

    internal fun onRelease() {
        if (!this._refreshing) {
            if (adjustedDistancePulled > threshold) {
                onRefreshState.value()
            } else {
                animateIndicatorTo(0f)
            }
        }
        distancePulled = 0f
    }

    internal fun setRefreshing(refreshing: Boolean) {
        if (this._refreshing != refreshing) {
            this._refreshing = refreshing
            this.distancePulled = 0f
            animateIndicatorTo(if (refreshing) refreshingOffset else 0f)
        }
    }

    private fun animateIndicatorTo(offset: Float) = animationScope.launch {
        animate(initialValue = _position, targetValue = offset) { value, _ ->
            _position = value
        }
    }

    private fun calculateIndicatorPosition(): Float = when {
        adjustedDistancePulled <= threshold -> adjustedDistancePulled
        else -> {
            val overshootPercent = abs(progress) - 1.0f
            val linearTension = overshootPercent.coerceIn(0f, 2f)
            val tensionPercent = linearTension - linearTension.pow(2) / 4
            val extraOffset = threshold * tensionPercent
            threshold + extraOffset
        }
    }
}
