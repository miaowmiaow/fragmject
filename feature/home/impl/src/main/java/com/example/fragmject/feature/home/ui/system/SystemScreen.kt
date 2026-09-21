package com.example.fragmject.feature.home.ui.system

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.TitleBar
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.feature.home.mapper.toFeedCardUiState
import com.example.fragmject.feature.home.nav.HomeNavActions
import com.example.fragmject.core.model.Tree

@Composable
fun SystemScreen(
    cid: String,
    systemViewModel: SystemViewModel = viewModel(),
    actions: HomeNavActions = HomeNavActions(),
    onNavigateUp: () -> Unit = {},
) {
    val treeResult by systemViewModel.treeResult.collectAsStateWithLifecycle()
    // getTree 会遍历整个 treeResult 查找 cid；用 remember(cid, treeResult) 缓存，
    // 避免 SystemScreen 因分页加载/翻页等高频重组时重复执行 O(n*m) 遍历。
    val treeData = remember(cid, treeResult) { treeResult.getTree(cid) }
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
            HorizontalPager(state = pagerState) { page ->
                val pageCid = treeData.third[page].id
                val listState = rememberLazyListState()
                val pagingItems = remember(pageCid) {
                    systemViewModel.pagingFlow(pageCid)
                }.collectAsLazyPagingItems()
                PagingSwipeRefreshBox(
                    pagingItems = pagingItems,
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    key = { it.id },
                ) { item ->
                    FeedCard(
                        data = remember(item.id) { item.toFeedCardUiState() },
                        onItemClick = actions.onArticleClick,
                        onUserClick = actions.onAuthorClick,
                        onFooterClick = actions.onChapterClick,
                        onToggleClick = systemViewModel::collect,
                    )
                }
            }
        }
    }

}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SystemScreenPreview() {
    AppTheme { SystemScreen("") }
}

/**
 * 在体系树中按 cid 定位：返回 (目标子项下标, 父章节名, 子项列表)。
 * 未命中时返回默认 (0, "体系", 空列表)。
 */
private fun List<Tree>.getTree(cid: String): Triple<Int, String, List<Tree>> {
    for (tree in this) {
        tree.children?.forEachIndexed { index, data ->
            if (data.id == cid) return Triple(index, tree.name, tree.children.orEmpty())
        }
    }
    return Triple(0, "体系", emptyList())
}