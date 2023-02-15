package com.example.fragment.module.wan.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.library.common.bean.ProjectTreeBean
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.model.ProjectTreeViewModel
import com.example.fragment.module.wan.model.ProjectViewModel
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ProjectScreen(
    viewModel: ProjectTreeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(viewModel.getTabIndex())
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateTabIndex(pagerState.currentPage)
        }
    }
    if (uiState.isLoading) {
        FullScreenLoading()
    } else {
        Column {
            ProjectTab(
                pagerState = pagerState,
                onTabClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
                data = uiState.result
            )
            ProjectPager(uiState.result, pagerState)
        }
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
    tabData: List<ProjectTreeBean>,
    pagerState: PagerState,
    viewModel: ProjectViewModel = viewModel()
) {
    if (tabData.isNotEmpty()) {
        HorizontalPager(
            count = tabData.size,
            state = pagerState,
        ) { page ->
            val cid = tabData[page].id
            LaunchedEffect(Unit) {
                viewModel.init(cid)
            }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val listState = rememberLazyListState(
                viewModel.getListIndex(cid),
                viewModel.getListScrollOffset(cid)
            )
            DisposableEffect(Unit) {
                onDispose {
                    viewModel.updateListIndex(listState.firstVisibleItemIndex, cid)
                    viewModel.updateListScrollOffset(
                        listState.firstVisibleItemScrollOffset,
                        cid
                    )
                }
            }
            if (uiState.getRefreshing(cid) && !uiState.getLoading(cid)) {
                FullScreenLoading()
            } else {
                SwipeRefresh(
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    refreshing = uiState.getRefreshing(cid),
                    loading = uiState.getLoading(cid),
                    onRefresh = { viewModel.getHome(cid) },
                    onLoad = { viewModel.getNext(cid) },
                    onRetry = { viewModel.getHome(cid) },
                    data = uiState.getResult(cid),
                ) { _, item ->
                    ArticleCard(item = item)
                }
            }
        }
    }
}
