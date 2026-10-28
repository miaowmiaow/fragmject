package com.example.fragmject.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.ui.R

/**
 * Paging 3 版下拉刷新 & 加载更多容器（纯列表）。
 *
 * 只负责「下拉刷新 + 列表 + 追加态」。首屏的加载/空/错误三态由 [ContentStateBox] 承载，
 * 调用方通过 [contentPhase] 从 [LazyPagingItems] 推导三态后在外层组合。
 *
 * @param pagingItems 由 collectAsLazyPagingItems() 得到的可分页数据
 * @param itemContent 列表项渲染
 */
@Composable
fun <T : Any> PagingSwipeRefreshBox(
    pagingItems: LazyPagingItems<T>,
    modifier: Modifier = Modifier,
    threshold: Dp = 100.dp,
    enablePullRefresh: Boolean = true,
    onRefresh: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    headerContent: (LazyListScope.() -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append
    val state = rememberPullToRefreshState()
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    // 仅用户主动下拉刷新时才显示刷新指示器；初始加载/搜索等程序触发的 refresh
    // 由外层 ContentStateBox 的骨架屏表达，二者不应叠加。
    var isPullRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshState) {
        if (refreshState !is LoadState.Loading) {
            isPullRefreshing = false
        }
    }
    Box(
        modifier = modifier
            .pullToRefresh(
                state = state,
                isRefreshing = isPullRefreshing,
                onRefresh = {
                    isPullRefreshing = true
                    pagingItems.refresh()
                    onRefresh?.invoke()
                },
                enabled = enablePullRefresh,
            )
            .clipToBounds()
            .background(AppColors.refreshContainer)
            .graphicsLayer {
                translationY = state.distanceFraction * thresholdPx
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        RefreshIndicator(isPullRefreshing, threshold) { state.distanceFraction }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
            ) {
                headerContent?.invoke(this)
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey(key),
                    contentType = pagingItems.itemContentType(contentType),
                ) { index ->
                    pagingItems[index]?.let { itemContent(it) }
                }
                item {
                    AppendStateFooter(appendState) { pagingItems.retry() }
                }
            }
        }
    }
}

/**
 * 从 [LazyPagingItems] 推导首屏内容四态。
 *
 * 判定逻辑（含「首帧不算空态」的原因）见 [contentPhaseOf]，这里只做转发。
 */
fun <T : Any> LazyPagingItems<T>.contentPhase(): ContentPhase =
    contentPhaseOf(loadState, itemCount)

/**
 * Paging 底部追加态指示器。
 *
 * 区分 [LoadState] 的追加态：
 * - [LoadState.Loading]：正在加载下一页
 * - [LoadState.Error]：加载失败，点击可重试
 * - [LoadState.NotLoading] 且到底：没有更多了
 * - [LoadState.NotLoading] 且未到底：不渲染（等待 LazyColumn 滚动触发下一页）
 */
@Composable
private fun AppendStateFooter(
    appendState: LoadState,
    onRetry: () -> Unit,
) {
    when (appendState) {
        is LoadState.Loading -> FooterText("正在加载中...")
        is LoadState.Error -> FooterText("加载失败，点击重试", onClick = onRetry)
        is LoadState.NotLoading ->
            if (appendState.endOfPaginationReached) {
                FooterText("没有更多了！")
            }
    }
}

@Composable
private fun FooterText(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = AppColors.refreshHintText,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

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

    // 帧资源一次性解码并复用：painterResource 以 resId 为 remember key，
    // 若按 id 现取现用，拖动时 id 每帧变化会触发「每帧一次 webp 解码（主线程同步）」；
    // 刷新中的 infinite 动画也有约 25 次/秒换帧。这里一次性建好 37 个 Painter 后只做索引切换。
    // 代价：首个使用该指示器的页面一次性解码 37 张小图（约几十 ms），之后恒定 O(1)。
    val painters = ArrayList<Painter>(RefreshingResIds.size)
    for (resId in RefreshingResIds) {
        painters.add(painterResource(resId))
    }

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
        painter = painters[id],
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer {
                translationY = (-position - loadingHeightPx) * 0.5f
            }
            .size(threshold, threshold),
        contentScale = ContentScale.Crop,
    )
}
