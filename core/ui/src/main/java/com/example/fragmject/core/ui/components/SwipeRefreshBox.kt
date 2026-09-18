package com.example.fragmject.core.ui.components

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.ui.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val RefreshingResIds by lazy {
    listOf(
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
}

/** 列表展示阶段：三态互斥，避免布尔交叉判断导致 EmptyContent/SkeletonList 误闪。 */
private enum class ListPhase {
    Loading,  // 骨架屏
    Empty,    // 空态
    Content,  // 真实列表
}

/**
 * 自定义下拉刷新&加载更多
 * @param items         列表数据
 * @param isRefreshing  设置下拉刷新
 * @param hasMore       设置加载更多
 * @param isFinishing   结束加载更多
 * @param onRefresh     下拉刷新回调
 * @param onLoad        加载更多回调
 */
@Composable
fun <T> SwipeRefreshBox(
    items: List<T>?,
    isRefreshing: Boolean,
    hasMore: Boolean,
    isFinishing: Boolean,
    onRefresh: () -> Unit,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Dp = 100.dp,
    enablePullRefresh: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    // 最小骨架屏显示时长：数据极快返回时，骨架屏也至少停留 350ms 再淡出，避免一闪而过
    val minSkeletonMillis = 350L
    // 骨架屏开始显示的时间戳
    var skeletonShownAt by remember { mutableLongStateOf(0L) }
    // 首次加载是否已完成（曾出现列表数据或确定空态），用于拆分 isFinishing 的「空态」语义
    var hasLoadedOnce by remember { mutableStateOf(false) }

    // 目标阶段：三态互斥，空态判定不再依赖 isFinishing（分页到底）
    val targetPhase = when {
        !items.isNullOrEmpty() -> ListPhase.Content
        hasLoadedOnce -> ListPhase.Empty
        isFinishing -> ListPhase.Empty
        else -> ListPhase.Loading
    }

    // 实际展示阶段（守卫后）
    var phase by remember { mutableStateOf(targetPhase) }

    LaunchedEffect(targetPhase) {
        when (targetPhase) {
            ListPhase.Loading -> {
                // 进入骨架屏：立即显示，并记录开始时间
                skeletonShownAt = SystemClock.uptimeMillis()
                phase = ListPhase.Loading
            }
            ListPhase.Empty -> {
                // 确定空态：标记首次加载已完成
                hasLoadedOnce = true
                phase = ListPhase.Empty
            }
            ListPhase.Content -> {
                // 数据到达：标记首次加载已完成，并保证骨架屏至少已显示 minSkeletonMillis
                hasLoadedOnce = true
                val elapsed = SystemClock.uptimeMillis() - skeletonShownAt
                val remaining = minSkeletonMillis - elapsed
                if (remaining > 0) delay(remaining.milliseconds)
                phase = ListPhase.Content
            }
        }
    }

    Crossfade(
        targetState = phase,
        modifier = modifier,
        label = "list_loading_content",
    ) { current ->
        when (current) {
            ListPhase.Loading -> {
                SkeletonList()
            }
            ListPhase.Empty -> {
                EmptyContent(onClick = onRefresh)
            }
            else -> {
                val data = items.orEmpty()
                val state = rememberPullToRefreshState()
                Box(
                    modifier = Modifier
                        .pullToRefresh(
                            state = state,
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            enabled = enablePullRefresh,
                        )
                        .clipToBounds()
                        .background(Color(0xFF010101))
                        .graphicsLayer {
                            translationY = state.distanceFraction * threshold.roundToPx()
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    RefreshIndicator(isRefreshing, threshold) {
                        state.distanceFraction
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxSize()
                    ) {
                        val shouldLoadMore by remember(listState, data.size, hasMore, isFinishing) {
                            derivedStateOf {
                                if (!hasMore || isFinishing || data.isEmpty()) return@derivedStateOf false
                                val lastVisibleIndex =
                                    listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                // 要求"最后一项必须已上屏"，防止列表不足一屏时 MoreIndicator 被立即看到而触发连续翻页
                                val totalCount = listState.layoutInfo.totalItemsCount
                                lastVisibleIndex >= data.lastIndex - 3 && totalCount >= data.size + 1
                            }
                        }
                        LaunchedEffect(shouldLoadMore) {
                            if (shouldLoadMore) {
                                onLoad()
                            }
                        }
                        LazyColumn(
                            state = listState,
                            contentPadding = contentPadding,
                            verticalArrangement = verticalArrangement,
                        ) {
                            itemsIndexed(
                                items = data,
                                key = key,
                                contentType = contentType
                            ) { index, item ->
                                itemContent(index, item)
                            }
                            item {
                                MoreIndicator(isFinishing)
                            }
                        }
                    }
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
            color = Color(0xFF999999),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun RefreshIndicator(
    isRefreshing: Boolean,
    threshold: Dp = PullToRefreshDefaults.PositionalThreshold,
    distanceFraction: () -> Float
) {
    val loadingHeightPx = with(LocalDensity.current) {
        threshold.toPx()
    }
    val position = distanceFraction() * loadingHeightPx

    // 仅在下拉刷新进行中才启动无限循环动画；列表空闲时（isRefreshing=false）不创建动画，
    // 避免无限动画每帧 tick 造成的无效 CPU 开销与潜在重组。
    val id = if (isRefreshing) {
        val infiniteTransition = rememberInfiniteTransition(label = "SwipeRefresh")
        val loadingAnimate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = RefreshingResIds.size.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "loadingAnimate"
        )
        loadingAnimate.toInt() % RefreshingResIds.size
    } else {
        ((position * 0.5f) % RefreshingResIds.size).toInt()
    }

    Image(
        painter = painterResource(RefreshingResIds[id]),
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer {
                translationY = (-position - loadingHeightPx) * 0.5f
            }
            .size(threshold, threshold),
        contentScale = ContentScale.Crop,
    )
}