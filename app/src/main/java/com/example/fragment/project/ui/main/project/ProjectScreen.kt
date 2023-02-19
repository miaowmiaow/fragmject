package com.example.fragment.project.ui.main.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.library.base.compose.TabBar
import com.example.fragment.project.components.ArticleCard
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ProjectScreen(
    projectTreeViewModel: ProjectTreeViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUserInfo: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    Column {
        TabBar(
            pagerState = pagerState,
            data = projectTreeUiState.result,
            textMapping = { it.name },
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
        )
        HorizontalPager(
            count = projectTreeUiState.result.size,
            state = pagerState,
        ) { page ->
            val pageCid = projectTreeUiState.result[page].id
            val projectUiState by projectViewModel.uiState.collectAsStateWithLifecycle()
            val listState = rememberLazyListState()
            DisposableEffect(LocalLifecycleOwner.current) {
                projectViewModel.init(pageCid)
                onDispose {}
            }
            if (projectTreeUiState.isLoading
                || (projectUiState.getRefreshing(pageCid) && !projectUiState.getLoading(pageCid))
            ) {
                FullScreenLoading()
            } else {
                SwipeRefresh(
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    refreshing = projectUiState.getRefreshing(pageCid),
                    loading = projectUiState.getLoading(pageCid),
                    onRefresh = { projectViewModel.getHome(pageCid) },
                    onLoad = { projectViewModel.getNext(pageCid) },
                    onRetry = { projectViewModel.getHome(pageCid) },
                    data = projectUiState.getResult(pageCid),
                ) { _, item ->
                    ArticleCard(
                        item = item,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToSystem = onNavigateToSystem,
                        onNavigateToUserInfo = onNavigateToUserInfo,
                        onNavigateToWeb = onNavigateToWeb
                    )
                }
            }
        }
    }
}