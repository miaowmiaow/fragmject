package com.example.fragmject.feature.home.ui.project

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.SkeletonContent
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.TabBar
import kotlinx.coroutines.launch
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.feature.home.mapper.toFeedCardUiState
import com.example.fragmject.feature.home.nav.HomeNavActions

@Composable
fun ProjectScreen(
    projectTreeViewModel: ProjectTreeViewModel = viewModel(),
    projectListViewModel: ProjectListViewModel = viewModel(),
    actions: HomeNavActions = HomeNavActions(),
) {
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
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
                val pagingItems = remember(pageCid) {
                    projectListViewModel.pagingFlow(pageCid)
                }.collectAsLazyPagingItems()
                PagingSwipeRefreshBox(
                    pagingItems = pagingItems,
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { item ->
                    FeedCard(
                        data = remember(item.id) { item.toFeedCardUiState() },
                        onItemClick = actions.onArticleClick,
                        onUserClick = actions.onAuthorClick,
                        onFooterClick = actions.onChapterClick,
                        onToggleClick = projectListViewModel.collectAction,
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