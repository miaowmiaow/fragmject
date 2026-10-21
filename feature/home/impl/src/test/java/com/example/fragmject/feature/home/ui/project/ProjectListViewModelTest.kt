package com.example.fragmject.feature.home.ui.project

import androidx.paging.PagingData
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

/** 项目列表：同一 cid 的分页流必须复用，避免横滑切页丢失已加载数据。 */
class ProjectListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `paging flow is cached per cid`() = runTest {
        val repo = FakeProjectListRepository()

        val viewModel = ProjectListViewModel(repo, FakeCollectState())

        val first = viewModel.pagingFlow("60")
        assertSame(first, viewModel.pagingFlow("60"))
        assertEquals(listOf("60"), repo.requestedCids)

        val other = viewModel.pagingFlow("61")
        assertNotSame(first, other)
        assertEquals(listOf("60", "61"), repo.requestedCids)
    }
}

private class FakeProjectListRepository : ProjectRepository {
    val requestedCids = mutableListOf<String>()

    override fun observeProjectTree(): Flow<List<ProjectTree>> = emptyFlow()
    override suspend fun refreshProjectTree(): DomainResult<Unit> = DomainResult.Success(Unit)

    override fun getProjectPagingData(cid: String): Flow<PagingData<Article>> {
        requestedCids += cid
        return flowOf(PagingData.empty())
    }
}

private class FakeCollectState : CollectState {
    override val overrides: MutableStateFlow<Map<String, Boolean>> = MutableStateFlow(emptyMap())
    override val collectFailed: Flow<String> = emptyFlow()
    override fun toggle(id: String, collect: Boolean) = Unit
}
