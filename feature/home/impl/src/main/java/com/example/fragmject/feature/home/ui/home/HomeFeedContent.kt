package com.example.fragmject.feature.home.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.ui.components.CollectInteraction
import com.example.fragmject.core.ui.components.ContentPhase
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData

/**
 * 首页信息流的内容层入参：数据 + 收藏态 + 列表滚动状态。
 *
 * 与 [HomeViewModel] 解耦，让 UI 测试可以直接构造（`HomeFeedContentTest`），
 * 不必拉起 Hilt、网络与 Room。
 */
@Stable
internal class HomeFeedSpec(
    val banners: List<Banner>,
    val topArticles: List<Article>,
    val hasHeaderError: Boolean,
    val listState: LazyListState,
    val collectInteraction: CollectInteraction,
)

/** 首页信息流的回调集合：页面只负责把导航/刷新意图接到 ViewModel。 */
@Stable
internal class HomeFeedActions(
    val onArticleClick: (String) -> Unit,
    val onAuthorClick: (String) -> Unit,
    val onChapterClick: (String) -> Unit,
    val onCoverClick: (String) -> Unit,
    val onRetry: () -> Unit,
    val onRefreshHeader: () -> Unit,
)

/**
 * 首页信息流的纯内容层：加载/空/错误三态 + 头部（轮播/置顶）+ 分页列表。
 *
 * [phase] 由调用方推导后传入，避免这里再次读 Paging 的 LoadState 造成多余的快照读取。
 */
@Composable
internal fun HomeFeedContent(
    pagingItems: LazyPagingItems<Article>,
    phase: ContentPhase,
    spec: HomeFeedSpec,
    actions: HomeFeedActions,
) {
    ContentStateBox(
        phase = phase,
        modifier = Modifier.fillMaxSize(),
        onRetry = actions.onRetry,
    ) {
        PagingSwipeRefreshBox(
            pagingItems = pagingItems,
            modifier = Modifier.fillMaxSize(),
            listState = spec.listState,
            onRefresh = actions.onRefreshHeader,
            contentPadding = PaddingValues(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            key = { it.id },
            headerContent = { homeFeedHeader(spec, actions) },
        ) { item ->
            HomeFeedCard(item, spec, actions)
        }
    }
}

/**
 * 列表头部：轮播优先，轮播缺失且头部请求失败时给一条重试入口，最后是置顶文章。
 * 抽成 [LazyListScope] 扩展而不是 Composable：`headerContent` 的 DSL 需要在列表作用域内展开。
 */
private fun LazyListScope.homeFeedHeader(
    spec: HomeFeedSpec,
    actions: HomeFeedActions,
) {
    if (spec.banners.isNotEmpty()) {
        item(key = "banner") {
            BannerPager(
                data = spec.banners,
                pathMapping = { it.imagePath },
                // 用户正在滑动列表时暂停自动轮播：两条独立动画同时抢帧预算会
                // 放大掉帧，也让 scrollHome 的帧指标混入与滚动手势无关的帧。
                autoScrollEnabled = !spec.listState.isScrollInProgress,
                onClick = { _, banner -> actions.onArticleClick(banner.url) }
            )
        }
    } else if (spec.hasHeaderError) {
        // 头部失败且列表可见时给出提示：整页失败/空态已由 ContentStateBox 覆盖，
        // 这里只补「列表正常、头部静默失败」这一情形
        item(key = "header_error") {
            TextButton(onClick = actions.onRefreshHeader) {
                Text("头部内容加载失败，点击重试")
            }
        }
    }
    items(spec.topArticles, key = { "top_${it.id}" }) { article ->
        HomeFeedCard(article, spec, actions)
    }
}

@Composable
private fun HomeFeedCard(
    article: Article,
    spec: HomeFeedSpec,
    actions: HomeFeedActions,
) {
    FeedCard(
        data = rememberArticleFeedData(article, spec.collectInteraction.overrides),
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        onItemClick = actions.onArticleClick,
        onUserClick = actions.onAuthorClick,
        onFooterClick = actions.onChapterClick,
        onToggleClick = spec.collectInteraction.onToggleClick,
        onCoverClick = actions.onCoverClick,
    )
}
