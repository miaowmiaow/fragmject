package com.example.fragment.project.ui.main.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.project.R
import com.example.fragment.project.bean.ProjectTreeBean
import com.example.fragment.project.components.ArticleCard
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ProjectScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUserInfo: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    projectTreeViewModel: ProjectTreeViewModel = viewModel(),
) {
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(projectTreeViewModel.getTabIndex())
    DisposableEffect(Unit) {
        onDispose {
            projectTreeViewModel.updateTabIndex(pagerState.currentPage)
        }
    }
    Column {
        ProjectTab(
            pagerState = pagerState,
            onTabClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
            data = projectTreeUiState.result
        )
        ProjectPager(
            projectTreeUiState,
            pagerState = pagerState,
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToSystem = onNavigateToSystem,
            onNavigateToUserInfo = onNavigateToUserInfo,
            onNavigateToWeb = onNavigateToWeb,
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ProjectTab(
    pagerState: PagerState,
    onTabClick: (index: Int) -> Unit,
    data: List<ProjectTreeBean>?
) {
    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        backgroundColor = colorResource(R.color.white),
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                color = colorResource(R.color.theme)
            )
        },
        divider = {
            TabRowDefaults.Divider(color = colorResource(R.color.transparent))
        }
    ) {
        data?.forEachIndexed { index, item ->
            Tab(
                text = { Text(item.name) },
                onClick = { onTabClick(index) },
                selected = pagerState.currentPage == index,
                selectedContentColor = colorResource(R.color.theme),
                unselectedContentColor = colorResource(R.color.text_999)
            )
        }
    }
    TabRowDefaults.Divider(color = colorResource(R.color.line))
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ProjectPager(
    projectTreeUiState: ProjectTreeState,
    pagerState: PagerState,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUserInfo: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    projectViewModel: ProjectViewModel = viewModel(),
) {
    HorizontalPager(
        count = projectTreeUiState.result.size,
        state = pagerState,
    ) { page ->
        val cid = projectTreeUiState.result[page].id
        val projectUiState by projectViewModel.uiState.collectAsStateWithLifecycle()
        val listState = rememberLazyListState(
            projectViewModel.getListIndex(cid),
            projectViewModel.getListScrollOffset(cid)
        )
        DisposableEffect(LocalLifecycleOwner.current) {
            projectViewModel.init(cid)
            onDispose {
                projectViewModel.updateListIndex(listState.firstVisibleItemIndex, cid)
                projectViewModel.updateListScrollOffset(
                    listState.firstVisibleItemScrollOffset,
                    cid
                )
            }
        }
        if (projectTreeUiState.isLoading
            || (projectUiState.getRefreshing(cid) && !projectUiState.getLoading(cid))
        ) {
            FullScreenLoading()
        } else {
            SwipeRefresh(
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                refreshing = projectUiState.getRefreshing(cid),
                loading = projectUiState.getLoading(cid),
                onRefresh = { projectViewModel.getHome(cid) },
                onLoad = { projectViewModel.getNext(cid) },
                onRetry = { projectViewModel.getHome(cid) },
                data = projectUiState.getResult(cid),
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