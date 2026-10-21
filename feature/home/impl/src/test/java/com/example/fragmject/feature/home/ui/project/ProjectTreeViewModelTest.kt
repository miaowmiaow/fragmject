package com.example.fragmject.feature.home.ui.project

import androidx.paging.PagingData
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.testing.MainDispatcherRule
import com.example.fragmject.core.ui.utils.RefreshableUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 项目树的「缓存 + 刷新」状态契约：缓存先出，失败转错误态并保留到显式重试。
 */
class ProjectTreeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - cached tree becomes success`() = runTest {
        val repo = FakeProjectRepository(ProjectTree(id = "p1", name = "项目"))

        val viewModel = ProjectTreeViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue(state is RefreshableUiState.Success)
        assertEquals("p1", (state as RefreshableUiState.Success).data.single().id)
    }

    @Test
    fun `init - refresh failure enters error with the server message`() = runTest {
        val repo = FakeProjectRepository()
        repo.refreshResult = DomainResult.Failure("500", "boom")

        val viewModel = ProjectTreeViewModel(repo)

        assertEquals(RefreshableUiState.Error("boom"), viewModel.uiState.value)
    }

    @Test
    fun `cached emission does not override an error state before retry`() = runTest {
        val repo = FakeProjectRepository()
        repo.refreshResult = DomainResult.Failure("500", "boom")
        val viewModel = ProjectTreeViewModel(repo)

        // 错误态优先：刷新失败后即使 Room 还有旧数据，也应停留在错误页等待重试
        repo.trees.value = listOf(ProjectTree(id = "late"))

        assertEquals(RefreshableUiState.Error("boom"), viewModel.uiState.value)
    }

    @Test
    fun `refresh recovers to success when the retry writes room`() = runTest {
        val repo = FakeProjectRepository()
        repo.refreshResult = DomainResult.Failure("500", "boom")
        val viewModel = ProjectTreeViewModel(repo)
        assertEquals(RefreshableUiState.Error("boom"), viewModel.uiState.value)

        repo.refreshResult = DomainResult.Success(Unit)
        repo.nextOnSuccess = listOf(ProjectTree(id = "p2"))
        viewModel.refresh()

        val state = viewModel.uiState.value
        assertTrue(state is RefreshableUiState.Success)
        assertEquals("p2", (state as RefreshableUiState.Success).data.single().id)
        assertEquals(2, repo.refreshCalls)
    }
}

private class FakeProjectRepository(vararg initial: ProjectTree) : ProjectRepository {
    val trees = MutableStateFlow(initial.toList())
    var refreshResult: DomainResult<Unit> = DomainResult.Success(Unit)
    var nextOnSuccess: List<ProjectTree> = initial.toList()
    var refreshCalls = 0

    override fun observeProjectTree(): Flow<List<ProjectTree>> = trees

    override suspend fun refreshProjectTree(): DomainResult<Unit> {
        refreshCalls++
        val result = refreshResult
        if (result is DomainResult.Success) trees.value = nextOnSuccess
        return result
    }

    override fun getProjectPagingData(cid: String): Flow<PagingData<Article>> = error("unused")
}
