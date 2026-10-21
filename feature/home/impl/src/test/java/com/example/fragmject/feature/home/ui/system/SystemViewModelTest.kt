package com.example.fragmject.feature.home.ui.system

import androidx.paging.PagingData
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Tree
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

/**
 * 体系页契约：树数据跟随 Room 流；各 cid 的分页流按需创建并缓存复用。
 */
class SystemViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `tree follows the room flow`() = runTest {
        val repo = FakeSystemRepository(listOf(Tree(id = "t1", name = "体系")))

        val viewModel = SystemViewModel(repo, FakeCollectState())

        assertEquals("t1", viewModel.treeResult.value.single().id)
        repo.trees.value = listOf(Tree(id = "t2"))
        assertEquals("t2", viewModel.treeResult.value.single().id)
    }

    @Test
    fun `paging flow is cached per cid and created lazily`() = runTest {
        val repo = FakeSystemRepository()
        val viewModel = SystemViewModel(repo, FakeCollectState())

        assertEquals(emptyList<String>(), repo.requestedCids)

        val first = viewModel.pagingFlow("60")
        assertSame(first, viewModel.pagingFlow("60"))
        assertEquals(listOf("60"), repo.requestedCids)

        val other = viewModel.pagingFlow("61")
        assertNotSame(first, other)
        assertEquals(listOf("60", "61"), repo.requestedCids)
    }
}

private class FakeSystemRepository(initialTrees: List<Tree> = emptyList()) : SystemRepository {
    val trees = MutableStateFlow(initialTrees)
    val requestedCids = mutableListOf<String>()

    override fun observeSystemTree(): Flow<List<Tree>> = trees

    override suspend fun refreshSystemTree(): DomainResult<Unit> = DomainResult.Success(Unit)

    override fun getSystemPagingData(cid: String): Flow<PagingData<Article>> {
        requestedCids += cid
        return flowOf(PagingData.empty())
    }
}

private class FakeCollectState : CollectState {
    override val overrides: MutableStateFlow<Map<String, Boolean>> = MutableStateFlow(emptyMap())
    override val collectFailed: Flow<String> = emptyFlow()
    override fun toggle(id: String, collect: Boolean) = Unit
}
