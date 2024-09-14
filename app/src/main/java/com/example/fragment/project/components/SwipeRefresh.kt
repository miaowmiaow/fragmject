package com.example.fragment.project.components

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R

/**
 * 自定义下拉刷新&加载更多
 * @param items         列表数据
 * @param isRefreshing  设置下拉刷新
 * @param isLoading     设置加载更多
 * @param isFinishing   结束加载更多
 * @param onRefresh     下拉刷新回调
 * @param onLoad        加载更多回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeRefreshBox(
    items: List<T>?,
    isRefreshing: Boolean,
    isLoading: Boolean,
    isFinishing: Boolean,
    onRefresh: () -> Unit,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Dp = 100.dp,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    if (items.isNullOrEmpty()) {
        if (!isFinishing) {
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
        val state = rememberPullToRefreshState()
        Box(
            modifier = modifier
                .pullToRefresh(state = state, isRefreshing = isRefreshing, onRefresh = onRefresh)
                .clipToBounds()
                .background(colorResource(R.color.background_refresh))
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
                    .background(colorResource(R.color.background))
                    .fillMaxSize()
            ) {
                LazyColumn(
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
                        if (isLoading && items.size - index < 5) {
                            LaunchedEffect(items.size) { onLoad() }
                        }
                    }
                    item {
                        MoreIndicator(isFinishing)
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
            color = colorResource(R.color.theme),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshIndicator(
    isRefreshing: Boolean,
    threshold: Dp = PullToRefreshDefaults.PositionalThreshold,
    distanceFraction: () -> Float
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
    val loadingHeightPx = with(LocalDensity.current) {
        threshold.toPx()
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
    val position = distanceFraction() * loadingHeightPx
    val id =
        (if (isRefreshing) loadingAnimate else position * 0.5f) % refreshingResId.size
    Image(
        painter = painterResource(refreshingResId[id.toInt()]),
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer {
                translationY = (-position - loadingHeightPx) * 0.5f
            }
            .size(threshold, threshold),
        contentScale = ContentScale.Crop,
    )
}