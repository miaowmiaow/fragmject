package com.example.fragmject.feature.wan.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.common.TransitionGuard
import com.example.fragmject.core.data.collect.rememberCollectAction
import com.example.fragmject.core.designsystem.TitleBar
import com.example.fragmject.core.designsystem.WanTheme
import com.example.fragmject.core.ui.components.ArticleCard
import com.example.fragmject.core.ui.components.toArticleCardUiState
import com.example.fragmject.core.ui.components.LoadingContent
import com.example.fragmject.core.ui.components.SwipeRefreshBox
import com.example.fragmject.feature.wan.SystemNavKey
import com.example.fragmject.feature.wan.UserNavKey
import com.example.fragmject.feature.wan.WanUiState
import com.example.fragmject.feature.wan.WanViewModel
import com.example.fragmject.feature.wan.WebNavKey
import com.example.fragmject.feature.wan.isLoading

@Composable
fun SystemScreen(
    cid: String,
    wanViewModel: WanViewModel = viewModel(),
    systemViewModel: SystemViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val wanUiState by wanViewModel.uiState.collectAsStateWithLifecycle()
    val systemUiState by systemViewModel.uiState.collectAsStateWithLifecycle()
    val treeData = (wanUiState as? WanUiState.Success)?.getTree(cid) ?: Triple(0, "体系", listOf())
    // pageCount 用 lambda 读取，确保 treeData 异步加载完成后 PagerState 能感知到新的 size；
    // 由于 rememberPagerState 的 initialPage 仅在首次创建时生效，treeData 加载完后
    // 需要通过下面的 LaunchedEffect 主动 scrollToPage 才能正确高亮 / 翻到目标 index。
    val pagerState = rememberPagerState(initialPage = treeData.first) { treeData.third.size }
    LaunchedEffect(treeData.first, treeData.third.size) {
        val targetPage = treeData.first
        if (treeData.third.isNotEmpty() && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState.currentPage, treeData.third.size) {
        if (treeData.third.isEmpty()) return@LaunchedEffect
        val currentCid = treeData.third[pagerState.currentPage].id
        TransitionGuard.await()
        systemViewModel.init(currentCid)
    }
    Scaffold(
        topBar = {
            TitleBar(
                title = treeData.second,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LoadingContent(isLoading = wanUiState.isLoading) {
                HorizontalPager(state = pagerState) { page ->
                    val pageCid = treeData.third[page].id
                    val listState = rememberLazyListState()
                    SwipeRefreshBox(
                        items = systemUiState.getResult(pageCid),
                        isRefreshing = systemUiState.getRefreshing(pageCid),
                        hasMore = systemUiState.getLoading(pageCid),
                        isFinishing = systemUiState.getFinishing(pageCid),
                        onRefresh = { systemViewModel.getHome(pageCid) },
                        onLoad = { systemViewModel.getNext(pageCid) },
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        key = { _, item -> item.id },
                    ) { _, item ->
                        ArticleCard(
                            data = item.toArticleCardUiState(),
                            onArticleClick = { onNavigate(WebNavKey(it)) },
                            onUserClick = { onNavigate(UserNavKey(it)) },
                            onChapterClick = { onNavigate(SystemNavKey(it)) },
                            onTagClick = { onNavigate(SystemNavKey(it)) },
                            onCollectClick = rememberCollectAction(),
                        )
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SystemScreenPreview() {
    WanTheme { SystemScreen("") }
}