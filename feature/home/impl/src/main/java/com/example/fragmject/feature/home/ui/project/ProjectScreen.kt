package com.example.fragmject.feature.home.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.contentPhase
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.refreshablePhaseOf
import com.example.fragmject.core.ui.components.SkeletonRows
import com.example.fragmject.core.ui.components.TabBar
import com.example.fragmject.core.ui.utils.dataOrEmpty
import kotlinx.coroutines.launch
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData
import com.example.fragmject.core.navigation.contract.LocalNavigators

@Composable
fun ProjectScreen(
    projectTreeViewModel: ProjectTreeViewModel = hiltViewModel(),
    projectListViewModel: ProjectListViewModel = hiltViewModel(),
) {
    val navigators = LocalNavigators.current
    val onArticleClick = remember(navigators) { { url: String -> navigators.article.openArticle(url) } }
    val onAuthorClick = remember(navigators) { { userId: String -> navigators.user.openUserProfile(userId) } }
    val onChapterClick = remember(navigators) { { cid: String -> navigators.home.openSystemTree(cid) } }
    val onCoverClick = remember(navigators) {
        { url: String -> navigators.picture.openPicturePreview(listOf(url)) }
    }
    val projectTreeUiState by projectTreeViewModel.uiState.collectAsStateWithLifecycle()
    val categories = projectTreeUiState.dataOrEmpty()
    val scope = rememberCoroutineScope()
    val collectInteraction = rememberCollectInteraction(projectListViewModel.collectState)

    val treePhase = refreshablePhaseOf(projectTreeUiState) { it.isEmpty() }
    // Pager 必须常驻组合树：分类还没回来时也给它 1 个占位页（而不是用状态盒把它替换掉）。
    // 否则切 tab 时新页未加载会让 Pager 在滑动动画中途被移出，rememberPagerState 的 Saver
    // 把中断时的 currentPageOffsetFraction（≈0.5）恢复回来，数据到达后就停在
    // 「上一页一半、下一页一半」，且不会自动吸附。
    val pagerState = rememberPagerState { categories.size.coerceAtLeast(1) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabBar(
            data = categories,
            dataMapping = { it.name },
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            onClick = { scope.launch { pagerState.animateScrollToPage(it) } },
        )
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            // 全页唯一的状态盒：分类未就绪时这一页还没有 cid，直接呈现分类相位（骨架/错误/空态）；
            // 分类就绪后则由该页列表自己决定首屏。
            val listState = rememberLazyListState()
            val pagingItems = categories.getOrNull(page)?.let { tab ->
                remember(tab.id) { projectListViewModel.pagingFlow(tab.id) }.collectAsLazyPagingItems()
            }
            val phase = pagingItems?.contentPhase() ?: treePhase
            val onRetry: () -> Unit =
                if (pagingItems != null) pagingItems::retry else projectTreeViewModel::refresh
            ContentStateBox(
                phase = phase,
                modifier = Modifier.fillMaxSize(),
                loadingContent = { SkeletonRows(it) },
                onRetry = onRetry,
            ) {
                if (pagingItems != null) {
                    PagingSwipeRefreshBox(
                        pagingItems = pagingItems,
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
fun ProjectScreenPreview() {
    AppTheme { ProjectScreen() }
}
