package com.example.fragmject.feature.home.ui.home

import androidx.navigation3.runtime.NavKey
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.core.ui.components.ArticleCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.toArticleCardUiState
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.user.UserNavKey
import com.example.fragmject.feature.home.components.BannerPager

@Composable
fun HomeScreen(
    listState: LazyListState,
    viewModel: HomeViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
) {
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val topArticles by viewModel.topArticles.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
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
                        onClick = { _, banner -> onNavigate(WebNavKey(banner.url)) }
                    )
                }
            }
            items(topArticles, key = { "top_${it.id}" }) { article ->
                ArticleCard(
                    data = remember(article.id) { article.toArticleCardUiState() },
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    onArticleClick = { onNavigate(WebNavKey(it)) },
                    onUserClick = { onNavigate(UserNavKey(it)) },
                    onChapterClick = { onNavigate(SystemNavKey(it)) },
                    onTagClick = { onNavigate(SystemNavKey(it)) },
                    onCollectClick = viewModel.collectAction,
                )
            }
        },
    ) { item ->
        ArticleCard(
            data = remember(item.id) { item.toArticleCardUiState() },
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            onArticleClick = { onNavigate(WebNavKey(it)) },
            onUserClick = { onNavigate(UserNavKey(it)) },
            onChapterClick = { onNavigate(SystemNavKey(it)) },
            onTagClick = { onNavigate(SystemNavKey(it)) },
            onCollectClick = viewModel.collectAction,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun HomeScreenPreview() {
    AppTheme { HomeScreen(rememberLazyListState()) }
}