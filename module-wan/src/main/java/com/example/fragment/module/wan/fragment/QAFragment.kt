package com.example.fragment.module.wan.fragment

import android.net.Uri
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
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.PullRefreshLayout
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.model.TabEventViewMode
import com.example.fragment.module.wan.R
import com.example.fragment.module.wan.model.QAQuizModel
import com.example.fragment.module.wan.model.QASquareModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
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
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(eventViewModel.qaTabIndex())
        eventViewModel.setQATabIndex(pagerState.currentPage)

        Column {
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
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        onClick = {
                            scope.launch {
                                pagerState.scrollToPage(index)
                            }
                            eventViewModel.setQATabIndex(index)
                        },
                        selected = pagerState.currentPage == index,
                        selectedContentColor = colorResource(R.color.theme),
                        unselectedContentColor = colorResource(R.color.text_999)
                    )
                }
            }
            TabRowDefaults.Divider(color = colorResource(R.color.line))
            HorizontalPager(
                count = tabs.size,
                state = pagerState,
            ) { page ->
                val viewModel = when (page) {
                    0 -> {
                        val qaQuizModel: QAQuizModel = viewModel()
                        qaQuizModel
                    }
                    1 -> {
                        val qaSquareModel: QASquareModel = viewModel()
                        qaSquareModel
                    }
                    else -> throw ArrayIndexOutOfBoundsException("length=${tabs.size}; index=$page")
                }
                val listState = rememberLazyListState(
                    viewModel.pagerColumnItemIndex,
                    viewModel.pagerColumnItemScrollOffset
                )
                DisposableEffect(LocalLifecycleOwner.current) {
                    onDispose {
                        viewModel.pagerColumnItemIndex = listState.firstVisibleItemIndex
                        viewModel.pagerColumnItemScrollOffset =
                            listState.firstVisibleItemScrollOffset
                    }
                }
                PullRefreshLayout(
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    refreshing = viewModel.refreshing,
                    onRefresh = {
                        viewModel.getHome()
                    },
                    loading = viewModel.loading,
                    onLoad = {
                        viewModel.getNext()
                    },
                    items = viewModel.result,
                ) { index, item ->
                    ArticleCard(
                        index,
                        item,
                        onClick = {
                            navigation(Router.WEB, bundleOf(Keys.URL to Uri.encode(item.link)))
                        },
                        avatarClick = {
                            navigation(Router.SHARE_ARTICLE, bundleOf(Keys.UID to item.userId))
                        },
                        tagClick = {
                            val uriString = "https://www.wanandroid.com${item.tags?.get(0)?.url}"
                            val uri = Uri.parse(uriString)
                            var cid = uri.getQueryParameter(Keys.CID)
                            if (cid.isNullOrBlank()) {
                                val paths = uri.pathSegments
                                if (paths != null && paths.size >= 3) {
                                    cid = paths[2]
                                }
                            }
                            navigation(Router.SYSTEM, bundleOf(Keys.CID to cid.toString()))
                        },
                        chapterNameClick = {
                            navigation(Router.SYSTEM, bundleOf(Keys.CID to item.chapterId))
                        }
                    )
                }
            }
        }
    }
}