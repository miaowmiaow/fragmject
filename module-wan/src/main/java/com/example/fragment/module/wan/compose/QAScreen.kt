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
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.vm.QAViewModel
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun QAScreen(
    viewModel: QAViewModel = viewModel()
) {
    val tabs = arrayOf("问答", "广场")
    val pagerState = rememberPagerState(viewModel.getTabIndex())
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateTabIndex(pagerState.currentPage)
        }
    }
    Column {
        QATab(
            pagerState = pagerState,
            onTabClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
            data = tabs
        )
        QAPager(tabs = tabs, count = tabs.size, pagerState = pagerState)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun QATab(
    pagerState: PagerState,
    onTabClick: (index: Int) -> Unit,
    data: Array<String>
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
        data.forEachIndexed { index, text ->
            Tab(
                text = { Text(text) },
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
fun QAPager(
    tabs: Array<String>,
    count: Int,
    pagerState: PagerState,
    viewModel: QAViewModel = viewModel()
) {
    HorizontalPager(
        count = count,
        state = pagerState,
    ) { page ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            viewModel.init(tabs[page])
        }
        val listState = rememberLazyListState(
            viewModel.getListIndex(),
            viewModel.getListScrollOffset()
        )
        DisposableEffect(Unit) {
            onDispose {
                viewModel.updateListIndex(listState.firstVisibleItemIndex)
                viewModel.updateListScrollOffset(listState.firstVisibleItemScrollOffset)
            }
        }
        if (uiState.getRefreshing(tabs[page]) && !uiState.getLoading(tabs[page])) {
            FullScreenLoading()
        } else {
            SwipeRefresh(
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                refreshing = uiState.getRefreshing(tabs[page]),
                loading = uiState.getLoading(tabs[page]),
                onRefresh = { viewModel.getHome(tabs[page]) },
                onLoad = { viewModel.getNext(tabs[page]) },
                onRetry = { viewModel.getHome(tabs[page]) },
                data = uiState.getResult(tabs[page]),
            ) { _, item ->
                ArticleCard(item = item)
            }
        }
    }
}
