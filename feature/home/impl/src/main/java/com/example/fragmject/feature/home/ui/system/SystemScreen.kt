package com.example.fragmject.feature.home.ui.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.ui.components.TitleBar
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.designsystem.NAV_TRANSITION_DURATION_MS
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.contentPhase
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.model.Tree

@Composable
fun SystemScreen(
    cid: String,
    systemViewModel: SystemViewModel = hiltViewModel(),
) {
    val navigators = LocalNavigators.current
    val onArticleClick = remember(navigators) { { url: String -> navigators.article.openArticle(url) } }
    val onAuthorClick = remember(navigators) { { userId: String -> navigators.user.openUserProfile(userId) } }
    val onChapterClick = remember(navigators) { { cid: String -> navigators.home.openSystemTree(cid) } }
    val onCoverClick = remember(navigators) {
        { url: String -> navigators.picture.openPicturePreview(listOf(url)) }
    }
    val onNavigateUp = LocalOnNavigateUp.current
    val treeResult by systemViewModel.treeResult.collectAsStateWithLifecycle()
    val collectInteraction = rememberCollectInteraction(systemViewModel.collectState)
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
                onNavigateUp = onNavigateUp,
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
                ContentStateBox(
                    phase = pagingItems.contentPhase(),
                    modifier = Modifier.fillMaxSize(),
                    minShowMillis = NAV_TRANSITION_DURATION_MS,
                    onRetry = { pagingItems.retry() },
                ) {
                    PagingSwipeRefreshBox(
                        pagingItems = pagingItems,
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        key = { it.id },
                    ) { item ->
                        FeedCard(
                            data = rememberArticleFeedData(item, collectInteraction.overrides),
                            onItemClick = onArticleClick,
                            onUserClick = onAuthorClick,
                            onFooterClick = onChapterClick,
                            onToggleClick = collectInteraction.onToggleClick,
                            onCoverClick = onCoverClick,
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
    AppTheme { SystemScreen("") }
}

/**
 * 在体系树中按 cid 定位：返回 (目标子项下标, 目标子项名, 兄弟子项列表)。
 * 未命中时返回默认 (0, "体系", 空列表)。
 */
private fun List<Tree>.getTree(cid: String): Triple<Int, String, List<Tree>> {
    forEach { tree ->
        val children = tree.children ?: return@forEach
        children.forEachIndexed { index, data ->
            if (data.id == cid) return Triple(index, data.name, children)
        }
    }
    return Triple(0, "体系", emptyList())
}