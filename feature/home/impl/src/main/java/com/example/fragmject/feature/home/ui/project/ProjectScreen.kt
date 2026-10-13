package com.example.fragmject.feature.home.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.SkeletonContent
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.TabBar
import kotlinx.coroutines.launch
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.ui.mapper.toFeedCardUIState
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
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val collectInteraction = rememberCollectInteraction(projectListViewModel.collectState)
    when (val state = projectTreeUiState) {
        is ProjectTreeUiState.Error -> {
            ProjectTreeError(message = state.message, onRetry = projectTreeViewModel::refresh)
        }
        is ProjectTreeUiState.Loading -> {
            SkeletonContent(isLoading = true, modifier = Modifier.fillMaxSize()) {}
        }
        is ProjectTreeUiState.Success -> {
            val pagerState = rememberPagerState { state.result.size }
            Column {
                TabBar(
                    data = state.result,
                    dataMapping = { it.name },
                    pagerState = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    onClick = { scope.launch { pagerState.animateScrollToPage(it) } },
                )
                HorizontalPager(state = pagerState) { page ->
                    val pageCid = state.result[page].id
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
                            data = remember(item.id, collectInteraction.overrides[item.id]) { item.toFeedCardUIState(collectInteraction.overrides[item.id]) },
                            onItemClick = onArticleClick,
                            onUserClick = onAuthorClick,
                            onFooterClick = onChapterClick,
                            onToggleClick = collectInteraction.onToggleClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectTreeError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message.ifBlank { "加载失败，请重试" }, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun ProjectScreenPreview() {
    AppTheme { ProjectScreen() }
}