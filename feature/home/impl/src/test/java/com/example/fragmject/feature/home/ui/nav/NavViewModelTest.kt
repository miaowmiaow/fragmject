package com.example.fragmject.feature.home.ui.nav

import androidx.paging.PagingData
import com.example.fragmject.core.domain.repository.HomeNavRepository
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.NavTab
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.testing.MainDispatcherRule
import com.example.fragmject.core.ui.utils.RefreshableUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 导航页两条独立数据流的契约：任一刷新失败只影响自身状态，重试互不牵连。
 */
class NavViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - nav and tree caches both become success`() = runTest {
        val navRepo = FakeHomeNavRepository(listOf(NavTab(cid = "n1", name = "导航")))
        val treeRepo = FakeSystemRepository(listOf(Tree(id = "t1", name = "体系")))

        val viewModel = NavViewModel(navRepo, treeRepo)

        assertEquals("n1", (viewModel.navState.value as RefreshableUiState.Success).data.single().cid)
        assertEquals("t1", (viewModel.treeState.value as RefreshableUiState.Success).data.single().id)
    }

    @Test
    fun `nav refresh failure does not degrade the tree state`() = runTest {
        val navRepo = FakeHomeNavRepository(listOf(NavTab(cid = "n1")))
        navRepo.refreshResult = DomainResult.Failure("500", "nav boom")
        val treeRepo = FakeSystemRepository(listOf(Tree(id = "t1")))

        val viewModel = NavViewModel(navRepo, treeRepo)

        assertEquals(RefreshableUiState.Error("nav boom"), viewModel.navState.value)
        assertTrue(viewModel.treeState.value is RefreshableUiState.Success)
    }

    @Test
    fun `retrying nav keeps the tree untouched`() = runTest {
        val navRepo = FakeHomeNavRepository()
        navRepo.refreshResult = DomainResult.Failure("500", "nav boom")
        val treeRepo = FakeSystemRepository(listOf(Tree(id = "t1")))
        val viewModel = NavViewModel(navRepo, treeRepo)
        assertEquals(1, treeRepo.refreshCalls)

        navRepo.refreshResult = DomainResult.Success(Unit)
        navRepo.nextOnSuccess = listOf(NavTab(cid = "n2"))
        viewModel.retry()

        assertEquals("n2", (viewModel.navState.value as RefreshableUiState.Success).data.single().cid)
        assertEquals(1, treeRepo.refreshCalls)
    }

    @Test
    fun `tree refresh failure does not degrade the nav state and can be retried alone`() = runTest {
        val navRepo = FakeHomeNavRepository(listOf(NavTab(cid = "n1")))
        val treeRepo = FakeSystemRepository()
        treeRepo.refreshResult = DomainResult.Failure("500", "tree boom")
        val viewModel = NavViewModel(navRepo, treeRepo)

        assertEquals(RefreshableUiState.Error("tree boom"), viewModel.treeState.value)
        assertTrue(viewModel.navState.value is RefreshableUiState.Success)

        treeRepo.refreshResult = DomainResult.Success(Unit)
        treeRepo.nextOnSuccess = listOf(Tree(id = "t2"))
        viewModel.retryTree()

        assertEquals("t2", (viewModel.treeState.value as RefreshableUiState.Success).data.single().id)
        assertEquals(1, navRepo.refreshCalls)
    }
}

private class FakeHomeNavRepository(initial: List<NavTab> = emptyList()) : HomeNavRepository {
    val navs = MutableStateFlow(initial)
    var refreshResult: DomainResult<Unit> = DomainResult.Success(Unit)
    var nextOnSuccess: List<NavTab> = initial
    var refreshCalls = 0

    override fun observeNavigation(): Flow<List<NavTab>> = navs

    override suspend fun refreshNavigation(): DomainResult<Unit> {
        refreshCalls++
        val result = refreshResult
        if (result is DomainResult.Success) navs.value = nextOnSuccess
        return result
    }
}

private class FakeSystemRepository(initial: List<Tree> = emptyList()) : SystemRepository {
    val trees = MutableStateFlow(initial)
    var refreshResult: DomainResult<Unit> = DomainResult.Success(Unit)
    var nextOnSuccess: List<Tree> = initial
    var refreshCalls = 0

    override fun observeSystemTree(): Flow<List<Tree>> = trees

    override suspend fun refreshSystemTree(): DomainResult<Unit> {
        refreshCalls++
        val result = refreshResult
        if (result is DomainResult.Success) trees.value = nextOnSuccess
        return result
    }

    override fun getSystemPagingData(cid: String): Flow<PagingData<Article>> = flowOf(PagingData.empty())
}
