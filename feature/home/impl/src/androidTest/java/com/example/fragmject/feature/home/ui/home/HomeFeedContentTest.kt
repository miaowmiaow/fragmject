package com.example.fragmject.feature.home.ui.home

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.ui.components.CollectInteraction
import com.example.fragmject.core.ui.components.ContentPhase
import com.example.fragmject.core.ui.components.EMPTY_CONTENT_TAG
import com.example.fragmject.core.ui.components.SKELETON_LIST_TAG
import com.example.fragmject.core.ui.components.contentPhase
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 首页信息流的 Compose UI 测试：空态、条目点击、滚到底翻页。
 *
 * 只测 [HomeFeedContent] 这一层纯内容 Composable（数据与回调全部来自入参），
 * 因此不需要 Hilt、网络与 Room；ViewModel 侧由 JVM 单测覆盖。
 *
 * 用 `junit4.v2` 的 [createComposeRule]：v2 默认 `StandardTestDispatcher`，
 * 效应（`LaunchedEffect`、`collectAsLazyPagingItems` 的内部收集）**不会自动执行**，
 * 必须显式同步（[runOnIdle]）后断言；这也是它与 v1（`UnconfinedTestDispatcher`、
 * 立即执行）最大的差别。轮询条件里同样要先 `runOnIdle` 把待执行任务排空，
 * 否则条件永远读不到新状态。
 */
@RunWith(AndroidJUnit4::class)
class HomeFeedContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_showsEmptyPlaceholder() {
        composeRule.setContent {
            val flow = remember { flowOf(PagingData.empty<Article>()) }
            TestHost(pagingItems = flow.collectAsLazyPagingItems(), phase = ContentPhase.Empty)
        }
        composeRule.runOnIdle { }

        composeRule.onNodeWithTag(EMPTY_CONTENT_TAG).assertIsDisplayed()
    }

    /**
     * 首屏「还没开始加载」必须显示骨架屏，不能显示空态。
     *
     * 回归用例：`itemCount == 0` 曾被直接判成 Empty，冷启动（尤其安装后首次打开、
     * 首页预取尚未命中时）会先闪一帧「无数据」。这里用一个永不发射的流模拟该窗口，
     * 断言 phase 推导结果是 Loading。
     */
    @Test
    fun beforeFirstPageArrives_showsSkeleton() {
        composeRule.setContent {
            val pagingItems = remember { flow<PagingData<Article>> { awaitCancellation() } }
                .collectAsLazyPagingItems()
            TestHost(pagingItems = pagingItems, phase = pagingItems.contentPhase())
        }
        composeRule.runOnIdle { }

        composeRule.onNodeWithTag(SKELETON_LIST_TAG).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag(EMPTY_CONTENT_TAG).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun articleClick_reportsLink() {
        var clickedLink: String? = null
        val article = testArticle(1)
        composeRule.setContent {
            val flow = remember { flowOf(PagingData.from(listOf(article))) }
            TestHost(
                pagingItems = flow.collectAsLazyPagingItems(),
                phase = ContentPhase.Content,
                onArticleClick = { clickedLink = it },
            )
        }
        composeRule.runOnIdle { }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.runOnIdle { }
            composeRule.onAllNodesWithText(article.title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(article.title).performClick()

        assertEquals(article.link, clickedLink)
    }

    @Test
    fun scrollingToBottom_loadsNextPage() {
        val source = FakeArticlePagingSource(pageCount = 2, pageSize = 10)
        composeRule.setContent {
            val pager = remember {
                Pager(
                    config = PagingConfig(
                        pageSize = 10,
                        initialLoadSize = 10,
                        prefetchDistance = 1,
                        enablePlaceholders = false,
                    ),
                    pagingSourceFactory = { source },
                )
            }
            TestHost(pagingItems = pager.flow.collectAsLazyPagingItems(), phase = ContentPhase.Content)
        }
        composeRule.runOnIdle { }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.runOnIdle { }
            composeRule.onAllNodesWithText(testArticle(1).title).fetchSemanticsNodes().isNotEmpty()
        }
        // 滚到第一页末尾，触发 APPEND
        composeRule.onNode(hasScrollAction()).performScrollToIndex(9)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.runOnIdle { }
            source.appendRequests.get() > 0
        }
        assertTrue(source.appendRequests.get() > 0)

        // 第二页数据已接入列表，滚到最后一条能取到
        composeRule.onNode(hasScrollAction()).performScrollToIndex(19)
        composeRule.runOnIdle { }
        composeRule.onNodeWithText(testArticle(20).title).assertIsDisplayed()
    }

    private fun testArticle(index: Int): Article = Article(
        id = index.toString(),
        title = "标题-$index",
        link = "https://example.com/article/$index",
        desc = "",
        author = "作者",
        chapterName = "分类",
        niceDate = "刚刚",
        collect = false,
    )

    @Composable
    private fun TestHost(
        pagingItems: LazyPagingItems<Article>,
        phase: ContentPhase,
        onArticleClick: (String) -> Unit = {},
    ) {
        AppTheme {
            HomeFeedContent(
                pagingItems = pagingItems,
                phase = phase,
                spec = HomeFeedSpec(
                    banners = emptyList(),
                    topArticles = emptyList(),
                    hasHeaderError = false,
                    listState = rememberLazyListState(),
                    collectInteraction = CollectInteraction(
                        overrides = emptyMap(),
                        onToggleClick = { _, _ -> },
                    ),
                ),
                actions = HomeFeedActions(
                    onArticleClick = onArticleClick,
                    onAuthorClick = {},
                    onChapterClick = {},
                    onCoverClick = {},
                    onRetry = {},
                    onRefreshHeader = {},
                ),
            )
        }
    }

    /** 固定两页共 20 条的分页数据源，记录 APPEND 次数。 */
    private class FakeArticlePagingSource(
        private val pageCount: Int,
        private val pageSize: Int,
    ) : PagingSource<Int, Article>() {

        val appendRequests = AtomicInteger()

        override fun getRefreshKey(state: PagingState<Int, Article>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
            if (params is LoadParams.Append) appendRequests.incrementAndGet()
            val page = params.key ?: 0
            val start = page * pageSize + 1
            return LoadResult.Page(
                data = (start until start + pageSize).map { index ->
                    Article(
                        id = index.toString(),
                        title = "标题-$index",
                        link = "https://example.com/article/$index",
                        desc = "",
                    )
                },
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (page + 1 < pageCount) page + 1 else null,
            )
        }
    }
}
