package com.example.fragmject.core.domain.usecase

import androidx.paging.PagingData
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [HomeHeaderAggregateUseCase] 测试：验证 banner + 置顶文章并发聚合，以及失败传播。
 */
class HomeHeaderAggregateUseCaseTest {

    @Test
    fun `aggregates banners and top articles`() = runTest {
        val useCase = HomeHeaderAggregateUseCase(
            FakeHomeRepository(
                banners = DomainResult.Success(listOf(Banner(id = "1", title = "轮播"))),
                topArticles = DomainResult.Success(listOf(Article(id = "1", title = "置顶"))),
            )
        )

        val result = useCase()

        assertTrue(result is DomainResult.Success)
        val header = (result as DomainResult.Success).data
        assertEquals(1, header.banners.size)
        assertEquals("轮播", header.banners[0].title)
        assertEquals(1, header.topArticles.size)
        assertEquals("置顶", header.topArticles[0].title)
    }

    @Test
    fun `empty repository yields empty header`() = runTest {
        val useCase = HomeHeaderAggregateUseCase(FakeHomeRepository())

        val result = useCase()

        assertTrue(result is DomainResult.Success)
        val header = (result as DomainResult.Success).data
        assertEquals(0, header.banners.size)
        assertEquals(0, header.topArticles.size)
    }

    @Test
    fun `banner failure propagates`() = runTest {
        val useCase = HomeHeaderAggregateUseCase(
            FakeHomeRepository(
                banners = DomainResult.Failure(code = "500", message = "网络错误"),
            )
        )

        val result = useCase()

        assertTrue(result is DomainResult.Failure)
        assertEquals("500", (result as DomainResult.Failure).code)
    }
}

private class FakeHomeRepository(
    private val banners: DomainResult<List<Banner>> = DomainResult.Success(emptyList()),
    private val topArticles: DomainResult<List<Article>> = DomainResult.Success(emptyList()),
) : HomeRepository {
    override fun getHomePagingData(): Flow<PagingData<Article>> = flowOf(PagingData.empty())
    override suspend fun fetchHomeBanners(): DomainResult<List<Banner>> = banners
    override suspend fun fetchTopArticles(): DomainResult<List<Article>> = topArticles
}
