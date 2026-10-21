package com.example.fragmject.feature.collection.ui.mycollection

import androidx.paging.PagingData
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

/** 我的收藏：分页流来自仓库，收藏态由外部注入并原样透出。 */
class MyCollectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `exposes repository paging flow and collect state`() = runTest {
        val repo = FakeMyCollectRepository()
        val collectState = FakeCollectState()

        val viewModel = MyCollectViewModel(repo, collectState)

        assertSame(collectState, viewModel.collectState)
        assertEquals(1, repo.calls)
    }
}

private class FakeMyCollectRepository : MyCollectRepository {
    var calls = 0

    override fun getMyCollectPagingData(): Flow<PagingData<Article>> {
        calls++
        return flowOf(PagingData.empty())
    }
}

private class FakeCollectState : CollectState {
    override val overrides: MutableStateFlow<Map<String, Boolean>> = MutableStateFlow(emptyMap())
    override val collectFailed: Flow<String> = emptyFlow()
    override fun toggle(id: String, collect: Boolean) = Unit
}
