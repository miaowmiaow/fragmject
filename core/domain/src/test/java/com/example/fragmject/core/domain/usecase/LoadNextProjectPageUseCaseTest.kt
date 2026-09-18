package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadNextProjectPageUseCaseTest {

    private class FakeProjectRepository(
        private val nextPageResult: DomainResult<PageData>,
    ) : ProjectRepository {
        override fun observeProjectTree(): Flow<List<ProjectTree>> = flowOf(emptyList())
        override fun observeProjectArticles(cid: String): Flow<List<Article>> = flowOf(emptyList())
        override suspend fun refreshProjectTree(): DomainResult<Unit> = DomainResult.Success(Unit)
        override suspend fun refreshProjectArticles(cid: String): DomainResult<Int> = DomainResult.Success(0)
        override suspend fun loadProjectNextPage(cid: String, page: Int): DomainResult<PageData> = nextPageResult
    }

    @Test
    fun `success - forwards page data`() = runBlocking {
        val data = PageData(listOf(Article(id = "1")), 6)
        val useCase = LoadNextProjectPageUseCase(FakeProjectRepository(DomainResult.Success(data)))
        val result = useCase("cid", 2)
        assertTrue(result is DomainResult.Success)
        assertEquals(data, (result as DomainResult.Success).data)
    }

    @Test
    fun `failure - forwards error`() = runBlocking {
        val failure = DomainResult.Failure("500", "boom")
        val useCase = LoadNextProjectPageUseCase(FakeProjectRepository(failure))
        val result = useCase("cid", 2)
        assertTrue(result is DomainResult.Failure)
        assertEquals("500", (result as DomainResult.Failure).code)
    }
}
