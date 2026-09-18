package com.example.fragmject.feature.home.ui.home

import androidx.navigation3.runtime.NavKey
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.core.ui.components.ArticleCard
import com.example.fragmject.core.ui.components.toArticleCardUiState
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.user.UserNavKey
import com.example.fragmject.feature.home.components.BannerPager
import com.example.fragmject.core.ui.components.SwipeRefreshBox

@Composable
fun HomeScreen(
    listState: LazyListState,
    viewModel: HomeViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SwipeRefreshBox(
        items = uiState.result,
        isRefreshing = uiState.isRefreshing,
        hasMore = uiState.isLoading,
        isFinishing = uiState.isFinishing,
        onRefresh = { viewModel.getHome(userTriggered = true) },
        onLoad = { viewModel.getNext() },
        modifier = Modifier.fillMaxSize(),
        listState = listState,
        contentPadding = PaddingValues(top = 10.dp),
        // banner 行用固定字符串作为 key；其余文章用业务 id。
        // 这样增量更新时 LazyColumn 能复用已上屏的 item 状态，避免不必要的重创建。
        key = { _, item -> if (item.viewType == 0) "banner" else item.id },
        contentType = { _, item -> item.viewType },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { _, item ->
        if (item.viewType == 0) {
            BannerPager(
                data = item.banners,
                pathMapping = { it.imagePath },
                onClick = { _, banner -> onNavigate(WebNavKey(banner.url)) }
            )
        } else {
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
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun HomeScreenPreview() {
    AppTheme { HomeScreen(rememberLazyListState()) }
}