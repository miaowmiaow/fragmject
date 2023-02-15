package com.example.fragment.module.wan.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.common.bean.SystemTreeBean
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.vm.SystemTreeViewModel
import com.example.fragment.module.wan.vm.SystemViewModel
import com.google.accompanist.pager.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

class SystemFragment : RouterFragment() {

    private var cid = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireArguments().takeIf {
            it.containsKey(Keys.CID)
        }?.let {
            cid = it.getString(Keys.CID, "0")
        }

        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    SystemScreen()
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
    fun SystemScreen(
        treeViewModel: SystemTreeViewModel = viewModel()
    ) {

        val statusBarColor = colorResource(R.color.theme)
        val systemUiController = rememberSystemUiController()

        val uiState by treeViewModel.uiState.collectAsStateWithLifecycle()
        val pagerState = rememberPagerState(treeViewModel.getTabIndex(cid))
        val coroutineScope = rememberCoroutineScope()
        SideEffect {
            treeViewModel.init(cid)
            systemUiController.setStatusBarColor(
                statusBarColor,
                darkIcons = false
            )
        }
        DisposableEffect(Unit) {
            onDispose {
                treeViewModel.updateTabIndex(pagerState.currentPage, cid)
            }
        }
        Column(
            modifier = Modifier
                .background(colorResource(R.color.background))
                .systemBarsPadding()
        ) {
            TitleBar(uiState.title)
            SystemTab(
                pagerState = pagerState,
                onTabClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
                data = uiState.result
            )
            SystemPager(uiState.result, pagerState)
        }
    }

    @Composable
    fun TitleBar(title: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(colorResource(R.color.theme))
        ) {
            IconButton(
                modifier = Modifier.height(45.dp),
                onClick = { onBackPressed() }
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colorResource(R.color.white)
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                color = colorResource(R.color.text_fff),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun SystemTab(
        pagerState: PagerState,
        onTabClick: (index: Int) -> Unit,
        data: List<SystemTreeBean>?
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
    fun SystemPager(
        tabData: List<SystemTreeBean>,
        pagerState: PagerState
    ) {
        if (tabData.isNotEmpty()) {
            HorizontalPager(
                count = tabData.size,
                state = pagerState,
            ) { page ->
                val cid = tabData[page].id
                val viewModel: SystemViewModel = viewModel()
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
                        onRefresh = {
                            viewModel.getHome(cid)
                        },
                        onLoad = {
                            viewModel.getNext(cid)
                        },
                        onRetry = {
                            viewModel.getHome(cid)
                        },
                        data = uiState.getResult(cid),
                    ) { _, item ->
                        ArticleCard(item = item)
                    }
                }
            }
        }
    }

}