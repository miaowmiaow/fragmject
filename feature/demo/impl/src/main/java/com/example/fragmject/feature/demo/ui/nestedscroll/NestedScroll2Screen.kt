package com.example.fragmject.feature.demo.ui.nestedscroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.TabBar
import com.example.fragmject.core.ui.components.contentPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 折叠头 + Tab + 下拉刷新 的嵌套滚动示例。
 *
 * 更推荐用 stickyHeader 实现同样效果，参考 NavScreen.kt 的 NavSystemContent。
 */
@Composable
fun NestedScroll2Screen() {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val data = remember {
        List(100) { "item $it" }
    }
    val pagingItems = remember(data) {
        flowOf(PagingData.from(data))
    }.collectAsLazyPagingItems()

    var boxHeightDp by remember { mutableStateOf(0.dp) }
    val topBarHeightDp = 100.dp
    val topBarHeightPx = with(density) { topBarHeightDp.toPx() }
    val tabBarHeightDp = 45.dp
    val tabBarHeightPx = with(density) { tabBarHeightDp.toPx() }

    val targetPercent = remember { Animatable(1f) }
    var headerFullyExpanded by remember { mutableStateOf(true) }

    val tabs = listOf("tab1", "tab2")
    val pagerState = rememberPagerState { tabs.size }
    val nestedScrollConnection = remember {
        CollapsingHeaderConnection(
            topBarHeightPx = topBarHeightPx,
            scope = scope,
            targetPercent = targetPercent,
            onHeaderFullyExpanded = { headerFullyExpanded = it },
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxHeightDp = with(density) { it.height.toDp() } }
            .nestedScroll(nestedScrollConnection),
    ) {
        CollapsingHeader(
            height = topBarHeightDp,
            heightPx = topBarHeightPx,
            targetPercent = targetPercent,
        )
        CollapsingTabBar(
            tabs = tabs,
            pagerState = pagerState,
            height = tabBarHeightDp,
            topBarHeightPx = topBarHeightPx,
            targetPercent = targetPercent,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(boxHeightDp - tabBarHeightDp)
                .graphicsLayer {
                    translationY = tabBarHeightPx + (topBarHeightPx * targetPercent.value)
                },
        ) { _ ->
            DemoPagingList(
                pagingItems = pagingItems,
                enablePullRefresh = headerFullyExpanded,
            )
        }
    }
}

/** 折叠头：上滑时整体上移并淡出。 */
@Composable
private fun CollapsingHeader(
    height: Dp,
    heightPx: Float,
    targetPercent: Animatable<Float, AnimationVector1D>,
) {
    Column(
        modifier = Modifier
            .background(AppColors.theme)
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                translationY = -(heightPx * (1 - targetPercent.value))
            },
    ) {
        Text(
            text = "嵌套滚动2",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = "Tab + 下拉刷新",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Tab 栏：随折叠进度贴着折叠头下沿移动。 */
@Composable
private fun CollapsingTabBar(
    tabs: List<String>,
    pagerState: PagerState,
    height: Dp,
    topBarHeightPx: Float,
    targetPercent: Animatable<Float, AnimationVector1D>,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                translationY = topBarHeightPx * targetPercent.value
            }
    ) {
        TabBar(
            onClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            data = tabs,
            dataMapping = { it },
            pagerState = pagerState,
        )
    }
}

/** 分页列表：头部完全展开时才允许下拉刷新。 */
@Composable
private fun DemoPagingList(
    pagingItems: LazyPagingItems<String>,
    enablePullRefresh: Boolean,
) {
    ContentStateBox(
        phase = pagingItems.contentPhase(),
        modifier = Modifier.fillMaxSize(),
        onRetry = { pagingItems.retry() },
    ) {
        PagingSwipeRefreshBox(
            pagingItems = pagingItems,
            modifier = Modifier.fillMaxSize(),
            enablePullRefresh = enablePullRefresh,
            key = { it },
        ) { item ->
            DemoListItem(item = item)
        }
    }
}

/** 单条示例数据。 */
@Composable
private fun DemoListItem(item: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item,
            fontSize = 14.sp,
            color = AppColors.orange,
        )
    }
    HorizontalDivider()
}

/**
 * 折叠头滚动衔接。
 *
 * 上滑（delta < 0）：先折叠头部，折叠完成后列表才开始滚动；
 * 下滑（delta > 0）：列表滚到顶后，剩余的下拉量用于展开头部。
 */
private class CollapsingHeaderConnection(
    private val topBarHeightPx: Float,
    private val scope: CoroutineScope,
    private val targetPercent: Animatable<Float, AnimationVector1D>,
    private val onHeaderFullyExpanded: (Boolean) -> Unit,
) : NestedScrollConnection {

    private var dyConsumed = 0f

    private fun consume(delta: Float): Offset {
        val newDy = (dyConsumed + delta).coerceIn(-topBarHeightPx, 0f)
        val actualConsumed = newDy - dyConsumed
        dyConsumed = newDy
        onHeaderFullyExpanded(newDy == 0f)
        val percent = dyConsumed / topBarHeightPx
        scope.launch {
            val targetValue = 1 - abs(percent.coerceIn(-1f, 0f))
            if (targetValue != targetPercent.value) {
                targetPercent.animateTo(targetValue)
            }
        }
        return Offset(0f, actualConsumed)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < 0) {
            return consume(available.y)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (available.y > 0) {
            return consume(available.y)
        }
        return Offset.Zero
    }
}
