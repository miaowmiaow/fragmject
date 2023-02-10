package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.model.QAModel
import com.google.accompanist.pager.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class QAFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): QAFragment {
            return QAFragment()
        }
    }

    private val tabs = arrayOf("问答", "广场")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    QAScreen()
                }
            }
        }
    }

    override fun initView() {}

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun QAScreen(
        viewModel: QAModel = viewModel()
    ) {
        val pagerState = rememberPagerState(viewModel.getTabIndex())

        val coroutineScope = rememberCoroutineScope()
        DisposableEffect(Unit) {
            onDispose {
                viewModel.updateTabIndex(pagerState.currentPage)
                coroutineScope.cancel()
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
            QAPager(count = tabs.size, pagerState = pagerState, viewModel = viewModel)
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
        count: Int,
        pagerState: PagerState,
        viewModel: QAModel
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

            DisposableEffect(LocalLifecycleOwner.current) {
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

}