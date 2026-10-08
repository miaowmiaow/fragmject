package com.example.fragmject.feature.home.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.feature.home.mapper.toFeedCardUIState
import com.example.fragmject.feature.home.nav.HomeNavActions
import com.example.fragmject.feature.home.components.BannerPager

@Composable
fun HomeScreen(
    listState: LazyListState,
    viewModel: HomeViewModel = viewModel(),
    actions: HomeNavActions = HomeNavActions(),
) {
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val topArticles by viewModel.topArticles.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val overrides by viewModel.collectState.overrides.collectAsStateWithLifecycle()
    val headerError by viewModel.headerError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.collectState.collectFailed.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
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
                        onClick = { _, banner -> actions.onArticleClick(banner.url) }
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
                    data = remember(article.id, overrides[article.id]) { article.toFeedCardUIState(overrides[article.id]) },
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    onItemClick = actions.onArticleClick,
                    onUserClick = actions.onAuthorClick,
                    onFooterClick = actions.onChapterClick,
                    onToggleClick = viewModel.collectState::toggle,
                )
            }
        },
    ) { item ->
        FeedCard(
            data = remember(item.id, overrides[item.id]) { item.toFeedCardUIState(overrides[item.id]) },
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            onItemClick = actions.onArticleClick,
            onUserClick = actions.onAuthorClick,
            onFooterClick = actions.onChapterClick,
            onToggleClick = viewModel.collectState::toggle,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun HomeScreenPreview() {
    AppTheme { HomeScreen(rememberLazyListState()) }
}