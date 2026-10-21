package com.example.fragmject.feature.collection.ui.myshare

import androidx.paging.PagingData
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
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

/** 我的分享：分页流来自仓库，收藏态由外部注入并原样透出。 */
class MyShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `exposes repository paging flow and collect state`() = runTest {
        val repo = FakeMyRepository()
        val collectState = FakeCollectState()

        val viewModel = MyShareViewModel(repo, collectState)

        assertSame(collectState, viewModel.collectState)
        assertEquals(1, repo.shareCalls)
    }
}

private class FakeMyRepository : MyRepository {
    var shareCalls = 0

    override suspend fun getUserCoin(): Coin? = null
    override fun getMyCoinPagingData(): Flow<PagingData<MyCoin>> = emptyFlow()
    override fun getMySharePagingData(): Flow<PagingData<Article>> {
        shareCalls++
        return flowOf(PagingData.empty())
    }

    override suspend fun shareArticle(title: String, link: String): SimpleResult = SimpleResult(true)
    override suspend fun collectArticle(id: String): SimpleResult = SimpleResult(true)
    override suspend fun uncollectArticle(id: String): SimpleResult = SimpleResult(true)
}

private class FakeCollectState : CollectState {
    override val overrides: MutableStateFlow<Map<String, Boolean>> = MutableStateFlow(emptyMap())
    override val collectFailed: Flow<String> = emptyFlow()
    override fun toggle(id: String, collect: Boolean) = Unit
}
