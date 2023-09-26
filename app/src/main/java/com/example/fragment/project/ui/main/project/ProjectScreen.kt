package com.example.fragment.project.ui.main.project

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.SwipeRefresh
import com.example.fragment.project.components.TabBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    projectTreeViewModel: ProjectTreeViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState { projectTreeUiState.result.size }
    TabBar(
        data = projectTreeUiState.result,
        textMapping = { it.name },
        pagerState = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(it) } },
    )
    TabRowDefaults.Divider(color = colorResource(R.color.line))
    HorizontalPager(state = pagerState) { page ->
        val pageCid = projectTreeUiState.result[page].id
        val projectUiState by projectViewModel.uiState.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    projectViewModel.init(pageCid)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        LoadingContent(
            projectTreeUiState.isLoading
                    || (projectUiState.getRefreshing(pageCid)
                    && !projectUiState.getLoading(pageCid))
        ) {
            SwipeRefresh(
                items = projectUiState.getResult(pageCid),
                refreshing = projectUiState.getRefreshing(pageCid),
                loading = projectUiState.getLoading(pageCid),
                finishing = projectUiState.getFinishing(pageCid),
                onRefresh = { projectViewModel.getHome(pageCid) },
                onLoad = { projectViewModel.getNext(pageCid) },
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                key = { _, item -> item.id },
            ) { _, item ->
                ArticleCard(
                    data = item,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToSystem = onNavigateToSystem,
                    onNavigateToUser = onNavigateToUser,
                    onNavigateToWeb = onNavigateToWeb
                )
            }
        }
    }
}