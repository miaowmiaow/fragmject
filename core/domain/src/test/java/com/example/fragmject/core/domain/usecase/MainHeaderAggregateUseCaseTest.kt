package com.example.fragmject.core.domain.usecase

import androidx.paging.PagingData
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MainHeaderAggregateUseCase] 测试：验证多 Repository 刷新的失败聚合策略。
 */
class MainHeaderAggregateUseCaseTest {

    @Test
    fun `refreshAll succeeds when both sources succeed`() = runTest {
        val useCase = MainHeaderAggregateUseCase(
            FakeSystemRepository(),
            FakeSearchRepository(),
        )

        val result = useCase.refreshAll()

        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `refreshAll surfaces navigation failure`() = runTest {
        val useCase = MainHeaderAggregateUseCase(
            FakeSystemRepository(refreshTreeResult = DomainResult.Failure("500", "tree boom")),
            FakeSearchRepository(),
        )

        val result = useCase.refreshAll()

        assertEquals(DomainResult.Failure("500", "tree boom"), result)
    }

    @Test
    fun `refreshAll surfaces search failure`() = runTest {
        val useCase = MainHeaderAggregateUseCase(
            FakeSystemRepository(),
            FakeSearchRepository(refreshHotKeyResult = DomainResult.Failure("503", "hot boom")),
        )

        val result = useCase.refreshAll()

        assertEquals(DomainResult.Failure("503", "hot boom"), result)
    }
}

private class FakeSystemRepository(
    private val refreshTreeResult: DomainResult<Unit> = DomainResult.Success(Unit),
) : SystemRepository {
    override fun observeSystemTree(): Flow<List<Tree>> = flowOf(emptyList())
    override suspend fun refreshSystemTree(): DomainResult<Unit> = refreshTreeResult
    override fun getSystemPagingData(cid: String): Flow<PagingData<Article>> = flowOf(PagingData.empty())
}

private class FakeSearchRepository(
    private val refreshHotKeyResult: DomainResult<Unit> = DomainResult.Success(Unit),
) : SearchRepository {
    override fun observeHotKey(): Flow<List<HotKey>> = flowOf(emptyList())
    override suspend fun refreshHotKey(): DomainResult<Unit> = refreshHotKeyResult
    override fun getSearchPagingData(key: String): Flow<PagingData<Article>> = flowOf(PagingData.empty())
}
