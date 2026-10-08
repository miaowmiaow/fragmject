package com.example.fragmject.feature.home.ui.project

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.fragmject.feature.home.mapper.toFeedCardUIState
import com.example.fragmject.feature.home.nav.HomeNavActions

@Composable
fun ProjectScreen(
    projectTreeViewModel: ProjectTreeViewModel = hiltViewModel(),
    projectListViewModel: ProjectListViewModel = hiltViewModel(),
    actions: HomeNavActions = HomeNavActions(),
) {
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val overrides by projectListViewModel.collectState.overrides.collectAsStateWithLifecycle()
    // 稳定回调：每次组合生成的 ::toggle 引用都是新实例，会让 FeedCard 无法跳过重组
    val onToggleClick = remember(projectListViewModel.collectState) { projectListViewModel.collectState::toggle }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        projectListViewModel.collectState.collectFailed.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
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
                            data = remember(item.id, overrides[item.id]) { item.toFeedCardUIState(overrides[item.id]) },
                            onItemClick = actions.onArticleClick,
                            onUserClick = actions.onAuthorClick,
                            onFooterClick = actions.onChapterClick,
                            onToggleClick = onToggleClick,
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