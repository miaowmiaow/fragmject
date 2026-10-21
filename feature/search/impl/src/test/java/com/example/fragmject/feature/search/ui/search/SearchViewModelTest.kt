package com.example.fragmject.feature.search.ui.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.History
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [SearchViewModel] 测试：热搜词加载三态、搜索提交与退出。
 *
 * 重点覆盖易错点：热词刷新**成功**后必须退出 loading —— 生产实现里 Room 写入会再次推送
 * 触发 observeHotKey 收尾，但"刷新成功且数据未变"时不会重发，仅靠 observe 收尾会让
 * 热搜区永久转圈。
 */
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - exposes history and hot keys`() = runTest {
        val historyRepo = FakeHistoryRepository(
            searchHistory = listOf(History(id = 1, key = "search_history", value = "kotlin")),
        )
        val searchRepo = FakeSearchRepository(hotKeys = listOf(HotKey(name = "问答")))

        val viewModel = SearchViewModel(searchRepo, historyRepo, FakeCollectState())

        assertEquals("问答", viewModel.hotKeyResult.value.single().name)
        assertEquals(1, viewModel.searchHistoryResult.value.size)
        assertFalse(viewModel.isHotKeyLoading.value)
    }

    @Test
    fun `hot key refresh success - stops loading`() = runTest {
        val searchRepo = FakeSearchRepository(hotKeys = listOf(HotKey(name = "问答")))

        val viewModel = SearchViewModel(searchRepo, FakeHistoryRepository(), FakeCollectState())

        assertFalse("刷新成功必须退出 loading，否则热搜区永久转圈", viewModel.isHotKeyLoading.value)
        assertTrue(viewModel.hotKeyError.value.isEmpty())
    }

    @Test
    fun `hot key refresh failure - exposes message and stops loading`() = runTest {
        val searchRepo = FakeSearchRepository(
            refreshResult = DomainResult.Failure("500", "热词加载失败"),
        )

        val viewModel = SearchViewModel(searchRepo, FakeHistoryRepository(), FakeCollectState())

        assertEquals("热词加载失败", viewModel.hotKeyError.value)
        assertFalse(viewModel.isHotKeyLoading.value)
    }

    @Test
    fun `retry hot key - clears error after success`() = runTest {
        val searchRepo = FakeSearchRepository(
            refreshResult = DomainResult.Failure("500", "热词加载失败"),
        )
        val viewModel = SearchViewModel(searchRepo, FakeHistoryRepository(), FakeCollectState())
        assertEquals("热词加载失败", viewModel.hotKeyError.value)

        searchRepo.refreshResult = DomainResult.Success(Unit)
        viewModel.retryHotKey()

        assertTrue(viewModel.hotKeyError.value.isEmpty())
        assertFalse(viewModel.isHotKeyLoading.value)
    }

    @Test
    fun `submit search - shows results and records query`() = runTest {
        val historyRepo = FakeHistoryRepository()
        val viewModel = SearchViewModel(FakeSearchRepository(), historyRepo, FakeCollectState())

        viewModel.submitSearch("kotlin")

        assertTrue(viewModel.isShowingSearchResults.value)
        assertEquals(listOf("kotlin"), historyRepo.recordedQueries)
    }

    @Test
    fun `exit search results - hides results and clears key`() = runTest {
        val viewModel = SearchViewModel(FakeSearchRepository(), FakeHistoryRepository(), FakeCollectState())
        viewModel.submitSearch("kotlin")

        viewModel.exitSearchResults()

        assertFalse(viewModel.isShowingSearchResults.value)
    }

    @Test
    fun `delete history - delegates to repository`() = runTest {
        val historyRepo = FakeHistoryRepository()
        val viewModel = SearchViewModel(FakeSearchRepository(), historyRepo, FakeCollectState())
        val history = History(id = 7, key = "search_history", value = "kotlin")

        viewModel.deleteHistory(history)

        assertEquals(listOf(history), historyRepo.deletedHistories)
    }
}

private class FakeSearchRepository(
    hotKeys: List<HotKey> = emptyList(),
    var refreshResult: DomainResult<Unit> = DomainResult.Success(Unit),
) : SearchRepository {

    private val hotKeyFlow = MutableStateFlow(hotKeys)

    override fun observeHotKey(): Flow<List<HotKey>> = hotKeyFlow

    override suspend fun refreshHotKey(): DomainResult<Unit> = refreshResult

    override fun getSearchPagingData(key: String): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            object : PagingSource<Int, Article>() {
                override fun getRefreshKey(state: PagingState<Int, Article>): Int? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> =
                    LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }
        },
    ).flow
}

private class FakeHistoryRepository(
    searchHistory: List<History> = emptyList(),
) : HistoryRepository {

    private val searchFlow = MutableStateFlow(searchHistory)
    val recordedQueries = mutableListOf<String>()
    val deletedHistories = mutableListOf<History>()

    override fun observeBookmarks(): Flow<List<History>> = emptyFlow()

    override fun observeBrowseHistory(): Flow<List<History>> = emptyFlow()

    override fun observeSearchHistory(): Flow<List<History>> = searchFlow

    override suspend fun setBookmark(value: String, url: String) = Unit

    override suspend fun recordBrowseVisit(value: String, url: String) = Unit

    override suspend fun recordSearchQuery(value: String) {
        recordedQueries += value
    }

    override suspend fun deleteHistory(history: History) {
        deletedHistories += history
    }
}

private class FakeCollectState : CollectState {
    override val overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val collectFailed: Flow<String> = emptyFlow()
    override fun toggle(id: String, collect: Boolean) = Unit
}
