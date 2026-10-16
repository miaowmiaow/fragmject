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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.ContentPhase
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.contentPhase
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.SkeletonRows
import com.example.fragmject.core.ui.components.TabBar
import com.example.fragmject.core.ui.utils.RefreshableUiState
import com.example.fragmject.core.ui.utils.dataOrEmpty
import kotlinx.coroutines.launch
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData
import com.example.fragmject.core.navigation.contract.LocalNavigators

@Composable
fun ProjectScreen(
    projectTreeViewModel: ProjectTreeViewModel = hiltViewModel(),
    projectListViewModel: ProjectListViewModel = hiltViewModel(),
) {
    val navigators = LocalNavigators.current
    val onArticleClick = remember(navigators) { { url: String -> navigators.article.openArticle(url) } }
    val onAuthorClick = remember(navigators) { { userId: String -> navigators.user.openUserProfile(userId) } }
    val onChapterClick = remember(navigators) { { cid: String -> navigators.home.openSystemTree(cid) } }
    val onCoverClick = remember(navigators) {
        { url: String -> navigators.picture.openPicturePreview(listOf(url)) }
    }
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val collectInteraction = rememberCollectInteraction(projectListViewModel.collectState)
    val phase = when (projectTreeUiState) {
        is RefreshableUiState.Loading -> ContentPhase.Loading
        is RefreshableUiState.Error -> ContentPhase.Error
        is RefreshableUiState.Success ->
            if (projectTreeUiState.dataOrEmpty().isEmpty()) ContentPhase.Empty else ContentPhase.Content
    }
    ContentStateBox(
        phase = phase,
        modifier = Modifier.fillMaxSize(),
        loadingContent = { SkeletonRows(it) },
        onRetry = projectTreeViewModel::refresh,
    ) {
        val pagerState = rememberPagerState { projectTreeUiState.dataOrEmpty().size }
        Column {
            TabBar(
                data = projectTreeUiState.dataOrEmpty(),
                dataMapping = { it.name },
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                onClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
            HorizontalPager(state = pagerState) { page ->
                val pageCid = projectTreeUiState.dataOrEmpty()[page].id
                val listState = rememberLazyListState()
                val pagingItems = remember(pageCid) {
                    projectListViewModel.pagingFlow(pageCid)
                }.collectAsLazyPagingItems()
                ContentStateBox(
                    phase = pagingItems.contentPhase(),
                    modifier = Modifier.fillMaxSize(),
                    onRetry = { pagingItems.retry() },
                ) {
                    PagingSwipeRefreshBox(
                        pagingItems = pagingItems,
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) { item ->
                        FeedCard(
                            data = rememberArticleFeedData(item, collectInteraction.overrides),
                            onItemClick = onArticleClick,
                            onUserClick = onAuthorClick,
                            onFooterClick = onChapterClick,
                            onToggleClick = collectInteraction.onToggleClick,
                            onCoverClick = onCoverClick,
                        )
                    }
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
