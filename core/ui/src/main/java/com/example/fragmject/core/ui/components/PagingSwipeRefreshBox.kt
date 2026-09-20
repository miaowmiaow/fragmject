package com.example.fragmject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey

/**
 * Paging 3 版下拉刷新 & 加载更多容器。
 *
 * 与 [SwipeRefreshBox] 的差异：数据源由 [LazyPagingItems] 驱动，
 * 分页边界（下一页 key / 是否到底 / 加载中 / 错误重试）由 Paging 3 的
 * [LoadState] 统一表达，调用方无需再传入 isRefreshing/isLoading/isFinishing。
 *
 * @param pagingItems 由 collectAsLazyPagingItems() 得到的可分页数据
 * @param itemContent 列表项渲染（复用现有视觉，仅数据驱动方式变化）
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
    val isRefreshing = refreshState is LoadState.Loading
    // 首次加载（尚无数据）才渲染骨架屏；已有数据时的下拉刷新由 RefreshIndicator 表达
    val isInitialLoading = isRefreshing && pagingItems.itemCount == 0
    // 刷新失败且无数据时视为错误态，与空态共用 EmptyContent，点击触发 retry
    val isError = refreshState is LoadState.Error && pagingItems.itemCount == 0
    val isEmpty = refreshState is LoadState.NotLoading && pagingItems.itemCount == 0

    when {
        isInitialLoading -> SkeletonList(modifier)
        isError || isEmpty -> EmptyContent(modifier = modifier) { pagingItems.retry() }
        else -> {
            val state = rememberPullToRefreshState()
            val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
            Box(
                modifier = modifier
                    .pullToRefresh(
                        state = state,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            pagingItems.refresh()
                            onRefresh?.invoke()
                        },
                        enabled = enablePullRefresh,
                    )
                    .clipToBounds()
                    .background(Color(0xFF010101))
                    .graphicsLayer {
                        translationY = state.distanceFraction * thresholdPx
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                RefreshIndicator(isRefreshing, threshold) { state.distanceFraction }
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
    }
}

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
            color = Color(0xFF999999),
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
