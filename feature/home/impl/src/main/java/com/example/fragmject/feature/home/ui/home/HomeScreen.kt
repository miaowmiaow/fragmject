package com.example.fragmject.feature.home.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData
import com.example.fragmject.feature.home.ui.home.BannerPager

@Composable
fun HomeScreen(
    listState: LazyListState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val navigators = LocalNavigators.current
    val onArticleClick = remember(navigators) { { url: String -> navigators.article.openArticle(url) } }
    val onAuthorClick = remember(navigators) { { userId: String -> navigators.user.openUserProfile(userId) } }
    val onChapterClick = remember(navigators) { { cid: String -> navigators.home.openSystemTree(cid) } }
    // 点封面开大图预览：网络图，纯看图（不可编辑）
    val onCoverClick = remember(navigators) {
        { url: String -> navigators.picture.openPicturePreview(listOf(url)) }
    }
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val topArticles by viewModel.topArticles.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val collectInteraction = rememberCollectInteraction(viewModel.collectState)
    val headerError by viewModel.headerError.collectAsStateWithLifecycle()
    PagingSwipeRefreshBox(
        pagingItems = pagingItems,
        modifier = Modifier.fillMaxSize(),
        listState = listState,
        onRefresh = viewModel::loadHeader,
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        key = { it.id },
        headerContent = {
            if (banners.isNotEmpty()) {
                item(key = "banner") {
                    BannerPager(
                        data = banners,
                        pathMapping = { it.imagePath },
                        onClick = { _, banner -> onArticleClick(banner.url) }
                    )
                }
            } else if (headerError != null) {
                // 头部失败且列表可见时给出提示：整页失败/空态已由 PagingSwipeRefreshBox
                // 的 EmptyContent 覆盖，这里只补「列表正常、头部静默失败」这一情形
                item(key = "header_error") {
                    TextButton(onClick = viewModel::loadHeader) {
                        Text("头部内容加载失败，点击重试")
                    }
                }
            }
            items(topArticles, key = { "top_${it.id}" }) { article ->
                FeedCard(
                    data = rememberArticleFeedData(article, collectInteraction.overrides),
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    onItemClick = onArticleClick,
                    onUserClick = onAuthorClick,
                    onFooterClick = onChapterClick,
                    onToggleClick = collectInteraction.onToggleClick,
                    onCoverClick = onCoverClick,
                )
            }
        },
    ) { item ->
        FeedCard(
            data = rememberArticleFeedData(item, collectInteraction.overrides),
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            onItemClick = onArticleClick,
            onUserClick = onAuthorClick,
            onFooterClick = onChapterClick,
            onToggleClick = collectInteraction.onToggleClick,
            onCoverClick = onCoverClick,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun HomeScreenPreview() {
    AppTheme { HomeScreen(rememberLazyListState()) }
}