package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadNextHomePageUseCaseTest {

    private class FakeHomeRepository(
        private val nextPageResult: DomainResult<PageData>,
    ) : HomeRepository {
        override fun observeHomeArticles(): Flow<List<Article>> = flowOf(emptyList())
        override fun observeHomeBanners(): Flow<List<Banner>> = flowOf(emptyList())
        override suspend fun refreshHome(): DomainResult<Int> = DomainResult.Success(0)
        override suspend fun loadNextPage(page: Int): DomainResult<PageData> = nextPageResult
    }

    @Test
    fun `success - forwards page data`() = runBlocking {
        val data = PageData(listOf(Article(id = "1")), 5)
        val useCase = LoadNextHomePageUseCase(FakeHomeRepository(DomainResult.Success(data)))
        val result = useCase(2)
        assertTrue(result is DomainResult.Success)
        assertEquals(data, (result as DomainResult.Success).data)
    }

    @Test
    fun `failure - forwards error`() = runBlocking {
        val failure = DomainResult.Failure("500", "boom")
        val useCase = LoadNextHomePageUseCase(FakeHomeRepository(failure))
        val result = useCase(2)
        assertTrue(result is DomainResult.Failure)
        assertEquals("500", (result as DomainResult.Failure).code)
    }
}
