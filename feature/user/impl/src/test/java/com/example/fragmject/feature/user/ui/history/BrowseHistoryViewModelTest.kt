package com.example.fragmject.feature.user.ui.history

import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.model.History
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 浏览历史页：书签与浏览记录两条流合并发布，写操作直接委托仓库。
 */
class BrowseHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - merges bookmarks and browse history`() = runTest {
        val repo = FakeHistoryRepository(
            bookmarks = flowOf(listOf(History(id = 1, key = "k1", value = "书签", url = "u1"))),
            history = flowOf(listOf(History(id = 2, key = "k2", value = "记录", url = "u2"))),
        )

        val viewModel = BrowseHistoryViewModel(repo)

        assertEquals(1L, viewModel.uiState.value.bookmarkResult.single().id)
        assertEquals(2L, viewModel.uiState.value.historyResult.single().id)
    }

    @Test
    fun `bookmark emission alone refreshes the bookmark list`() = runTest {
        val bookmarks = MutableStateFlow(listOf(History(id = 1, key = "k1", value = "旧书签")))
        val history = MutableStateFlow(listOf(History(id = 2, key = "k2", value = "记录")))
        val viewModel = BrowseHistoryViewModel(FakeHistoryRepository(bookmarks, history))

        // 只收藏/取消收藏：浏览记录流不发射，历史列表保留上一次的值
        bookmarks.value = listOf(History(id = 4, key = "k4", value = "新书签"))

        assertEquals(4L, viewModel.uiState.value.bookmarkResult.single().id)
        assertEquals(2L, viewModel.uiState.value.historyResult.single().id)
    }

    @Test
    fun `history emission alone refreshes the history list`() = runTest {
        val bookmarks = MutableStateFlow(listOf(History(id = 1, key = "k1", value = "书签")))
        val history = MutableStateFlow(listOf(History(id = 2, key = "k2", value = "旧记录")))
        val viewModel = BrowseHistoryViewModel(FakeHistoryRepository(bookmarks, history))

        // 对应「记录一次浏览」：只有浏览记录表变了。
        // 旧实现用 zip，单侧发射不会产生组合值，这条断言会失败。
        history.value = listOf(History(id = 3, key = "k3", value = "新记录"))

        assertEquals(3L, viewModel.uiState.value.historyResult.single().id)
        assertEquals(1L, viewModel.uiState.value.bookmarkResult.single().id)
    }

    @Test
    fun `record and delete delegate to the repository`() = runTest {
        val repo = FakeHistoryRepository()
        val viewModel = BrowseHistoryViewModel(repo)

        viewModel.recordBrowseVisit("标题", "https://a.com")
        viewModel.deleteHistory(History(id = 5, key = "k", value = "标题"))

        assertEquals(listOf("标题" to "https://a.com"), repo.recorded)
        assertEquals(listOf(5L), repo.deleted.map { it.id })
    }
}

private class FakeHistoryRepository(
    private val bookmarks: Flow<List<History>> = flowOf(emptyList()),
    private val history: Flow<List<History>> = flowOf(emptyList()),
) : HistoryRepository {
    val recorded = mutableListOf<Pair<String, String>>()
    val deleted = mutableListOf<History>()

    override fun observeBookmarks(): Flow<List<History>> = bookmarks
    override fun observeBrowseHistory(): Flow<List<History>> = history
    override fun observeSearchHistory(): Flow<List<History>> = flowOf(emptyList())
    override suspend fun setBookmark(value: String, url: String) = Unit
    override suspend fun recordBrowseVisit(value: String, url: String) {
        recorded += value to url
    }

    override suspend fun recordSearchQuery(value: String) = Unit
    override suspend fun deleteHistory(history: History) {
        deleted += history
    }
}
