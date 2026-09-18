package com.example.fragmject.feature.home.ui.project

import androidx.navigation3.runtime.NavKey
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.SkeletonContent
import com.example.fragmject.core.ui.components.SwipeRefreshBox
import com.example.fragmject.core.ui.components.TabBar
import kotlinx.coroutines.launch
import com.example.fragmject.core.ui.components.ArticleCard
import com.example.fragmject.core.ui.components.toArticleCardUiState
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.user.UserNavKey

@Composable
fun ProjectScreen(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    projectTreeViewModel: ProjectTreeViewModel = viewModel(),
    projectListViewModel: ProjectListViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
) {
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    // 在 Screen 顶层订阅一次，避免 HorizontalPager 每个 page 各自订阅同一 StateFlow
    // 造成多个重复订阅者；所有 page 共享同一份 state。
    val projectListUiState by projectListViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { projectTreeUiState.result.size }
    Column {
        TabBar(
            data = projectTreeUiState.result,
            dataMapping = { it.name },
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            onClick = { scope.launch { pagerState.animateScrollToPage(it) } },
        )
        SkeletonContent(isLoading = projectTreeUiState.isLoading) {
            HorizontalPager(state = pagerState) { page ->
                val pageCid = projectTreeUiState.result[page].id
                val listState = rememberLazyListState()
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_START) {
                            projectListViewModel.init(pageCid)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                SwipeRefreshBox(
                    items = projectListUiState.getResult(pageCid),
                    isRefreshing = projectListUiState.getRefreshing(pageCid),
                    hasMore = projectListUiState.getLoading(pageCid),
                    isFinishing = projectListUiState.getFinishing(pageCid),
                    onRefresh = { projectListViewModel.getHome(pageCid, userTriggered = true) },
                    onLoad = { projectListViewModel.getNext(pageCid) },
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { _, item ->
                    ArticleCard(
                        data = remember(item.id) { item.toArticleCardUiState() },
                        onArticleClick = { onNavigate(WebNavKey(it)) },
                        onUserClick = { onNavigate(UserNavKey(it)) },
                        onChapterClick = { onNavigate(SystemNavKey(it)) },
                        onTagClick = { onNavigate(SystemNavKey(it)) },
                        onCollectClick = projectListViewModel.collectAction,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun ProjectScreenPreview() {
    AppTheme { ProjectScreen() }
}