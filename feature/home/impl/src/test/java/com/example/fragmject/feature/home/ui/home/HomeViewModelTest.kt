package com.example.fragmject.feature.home.ui.home

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.HomeHeaderAggregateUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * [HomeViewModel] 测试：首页头部（banner + 置顶）的聚合结果消费。
 *
 * 覆盖三条路径：成功填充、失败保留旧值并暴露错误、失败后重试清除错误。
 * 头部拉取的并发编排在 [HomeHeaderAggregateUseCase] 内，这里用真实用例 + fake 端口验证。
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `header success - exposes banners and top articles`() = runTest {
        val repo = FakeHomeRepository(
            bannersResult = DomainResult.Success(listOf(Banner(id = "b1", title = "banner"))),
            topResult = DomainResult.Success(listOf(Article(id = "a1", title = "top"))),
        )

        val viewModel = createViewModel(repo)

        assertEquals("banner", viewModel.banners.value.single().title)
        assertEquals("top", viewModel.topArticles.value.single().title)
        assertNull(viewModel.headerError.value)
    }

    @Test
    fun `header failure - keeps previous data and exposes error`() = runTest {
        val repo = FakeHomeRepository(
            bannersResult = DomainResult.Success(listOf(Banner(id = "b1"))),
            topResult = DomainResult.Success(listOf(Article(id = "a1"))),
        )
        val viewModel = createViewModel(repo)

        repo.bannersResult = DomainResult.Failure("500", "banner failed")
        viewModel.loadHeader()

        // 失败不得静默清空已展示的头部数据，只追加错误态供 UI 提示/重试
        assertEquals(1, viewModel.banners.value.size)
        assertEquals(1, viewModel.topArticles.value.size)
        assertEquals("banner failed", viewModel.headerError.value?.message)
    }

    @Test
    fun `retry after failure - refreshes data and clears error`() = runTest {
        val repo = FakeHomeRepository(
            bannersResult = DomainResult.Failure("500", "banner failed"),
            topResult = DomainResult.Success(emptyList()),
        )
        val viewModel = createViewModel(repo)
        assertEquals("banner failed", viewModel.headerError.value?.message)

        repo.bannersResult = DomainResult.Success(listOf(Banner(id = "b2", title = "recovered")))
        viewModel.loadHeader()

        assertNull(viewModel.headerError.value)
        assertEquals("recovered", viewModel.banners.value.single().title)
    }

    private fun createViewModel(repo: HomeRepository): HomeViewModel =
        HomeViewModel(repo, HomeHeaderAggregateUseCase(repo), FakeCollectState())
}

private class FakeHomeRepository(
    var bannersResult: DomainResult<List<Banner>> = DomainResult.Success(emptyList()),
    var topResult: DomainResult<List<Article>> = DomainResult.Success(emptyList()),
) : HomeRepository {

    override fun warmUpFirstPage() = Unit

    override fun getHomePagingData(): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            object : PagingSource<Int, Article>() {
                override fun getRefreshKey(state: PagingState<Int, Article>): Int? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> =
                    LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }
        },
    ).flow

    override suspend fun fetchHomeBanners(): DomainResult<List<Banner>> = bannersResult

    override suspend fun fetchTopArticles(): DomainResult<List<Article>> = topResult
}

private class FakeCollectState : CollectState {
    override val overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val collectFailed: Flow<String> = emptyFlow()
    override fun toggle(id: String, collect: Boolean) = Unit
}
