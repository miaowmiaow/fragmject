package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.model.TabEventViewMode
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.model.QAModel
import com.example.fragment.module.wan.model.QAQuizModel
import com.example.fragment.module.wan.model.QASquareModel
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
        eventViewModel: TabEventViewMode = viewModel()
    ) {
        val pagerState = rememberPagerState(eventViewModel.qaTabIndex())

        eventViewModel.setQATabIndex(pagerState.currentPage)

        val coroutineScope = rememberCoroutineScope()
        DisposableEffect(Unit) {
            onDispose {
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
                    eventViewModel.setQATabIndex(it)
                },
                data = tabs
            )
            QAPager(count = tabs.size, pagerState = pagerState)
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
        pagerState: PagerState
    ) {
        HorizontalPager(
            count = count,
            state = pagerState,
        ) { page ->
            when (page) {
                0 -> {
                    val qaQuizModel: QAQuizModel = viewModel()
                    QAList(qaQuizModel)
                }
                1 -> {
                    val qaSquareModel: QASquareModel = viewModel()
                    QAList(qaSquareModel)
                }
                else -> throw ArrayIndexOutOfBoundsException("length=${count}; index=$page")
            }
        }
    }

    @Composable
    fun QAList(
        viewModel: QAModel
    ) {
        val listState = rememberLazyListState(
            viewModel.pagerItemIndex,
            viewModel.pagerItemScrollOffset
        )

        DisposableEffect(LocalLifecycleOwner.current) {
            onDispose {
                viewModel.pagerItemIndex = listState.firstVisibleItemIndex
                viewModel.pagerItemScrollOffset = listState.firstVisibleItemScrollOffset
            }
        }

        SwipeRefresh(
            modifier = Modifier.fillMaxSize(),
            listState = listState,
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            refreshing = viewModel.refreshing,
            loading = viewModel.loading,
            onRefresh = { viewModel.getHome() },
            onLoad = { viewModel.getNext() },
            onNoData = { viewModel.getHome() },
            data = viewModel.result,
        ) { _, item ->
            ArticleCard(item = item)
        }
    }
}