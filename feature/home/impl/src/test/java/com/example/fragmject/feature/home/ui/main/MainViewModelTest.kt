package com.example.fragmject.feature.home.ui.main

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * [MainViewModel] 测试：热词为 Room 唯一数据源，验证 observe 流驱动 hotKeys，
 * 以及网络刷新失败时的静默降级（保留缓存、不进入错误态）。
 */
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - collects hot keys from room`() = runTest {
        val searchRepo = FakeSearchRepository(hotKeys = listOf(HotKey(name = "问答")))

        val viewModel = MainViewModel(searchRepo)

        assertEquals(1, viewModel.hotKeys.value.size)
        assertEquals("问答", viewModel.hotKeys.value[0].name)
    }

    @Test
    fun `init - refresh failure keeps cached hot keys`() = runTest {
        val searchRepo = FakeSearchRepository(
            hotKeys = listOf(HotKey(name = "问答")),
            refreshHotKeyResult = DomainResult.Failure("500", "boom"),
        )

        val viewModel = MainViewModel(searchRepo)

        // 刷新失败静默降级：缓存热词保留，不进错误态
        assertEquals("问答", viewModel.hotKeys.value.single().name)
    }
}

private class FakeSearchRepository(
    hotKeys: List<HotKey> = emptyList(),
    var refreshHotKeyResult: DomainResult<Unit> = DomainResult.Success(Unit),
) : SearchRepository {
    private val hotKeyFlow = MutableStateFlow(hotKeys)

    override fun observeHotKey(): Flow<List<HotKey>> = hotKeyFlow
    override suspend fun refreshHotKey(): DomainResult<Unit> = refreshHotKeyResult
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
