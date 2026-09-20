package com.example.fragmject.feature.home.ui.main

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Navigation
import com.example.fragmject.core.model.Tree
import com.example.fragmject.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * [MainViewModel] 测试：验证 viewModelScope + Dispatchers.Main 被 [MainDispatcherRule]
 * 接管后，init 中的 Room Flow collect 与网络刷新能正确驱动 UiState。
 *
 * 这是 Phase 5 引入 kotlinx-coroutines-test 后解锁的首个 ViewModel 层测试。
 */
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - collects hot keys and system tree`() = runTest {
        val navigationRepo = FakeNavigationRepository(trees = listOf(Tree(id = "1", name = "体系")))
        val searchRepo = FakeSearchRepository(hotKeys = listOf(HotKey(name = "问答")))

        val viewModel = MainViewModel(navigationRepo, searchRepo)

        val state = viewModel.uiState.value as MainUiState.Success
        assertEquals(1, state.treeResult.size)
        assertEquals("体系", state.treeResult[0].name)
        assertEquals(1, state.hotKeyResult.size)
        assertEquals("问答", state.hotKeyResult[0].name)
        // 刷新完成后 isLoading 应被置为 false
        assertFalse(state.isLoading)
    }

    @Test
    fun `init - refresh failure still clears loading`() = runTest {
        val navigationRepo = FakeNavigationRepository(refreshTreeResult = DomainResult.Failure("500", "boom"))
        val searchRepo = FakeSearchRepository()

        val viewModel = MainViewModel(navigationRepo, searchRepo)

        val state = viewModel.uiState.value as MainUiState.Success
        // 失败路径也应把 isLoading 置 false，避免 UI 永久转圈
        assertFalse(state.isLoading)
    }
}

private class FakeNavigationRepository(
    trees: List<Tree> = emptyList(),
    private val refreshTreeResult: DomainResult<Unit> = DomainResult.Success(Unit),
) : NavigationRepository {
    private val treeFlow = MutableStateFlow(trees)

    override fun observeNavigation(): Flow<List<Navigation>> = flowOf(emptyList())
    override fun observeSystemTree(): Flow<List<Tree>> = treeFlow
    override suspend fun refreshNavigation(): DomainResult<Unit> = DomainResult.Success(Unit)
    override suspend fun refreshSystemTree(): DomainResult<Unit> = refreshTreeResult
}

private class FakeSearchRepository(
    hotKeys: List<HotKey> = emptyList(),
) : SearchRepository {
    private val hotKeyFlow = MutableStateFlow(hotKeys)

    override fun observeHotKey(): Flow<List<HotKey>> = hotKeyFlow
    override suspend fun refreshHotKey(): DomainResult<Unit> = DomainResult.Success(Unit)
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
