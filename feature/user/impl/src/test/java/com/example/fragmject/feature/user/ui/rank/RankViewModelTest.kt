package com.example.fragmject.feature.user.ui.rank

import androidx.paging.PagingData
import com.example.fragmject.core.domain.repository.CoinRankRepository
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 积分榜：分页流直接来自仓库，构造时只取一次。 */
class RankViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `exposes the coin rank paging flow`() = runTest {
        val repo = FakeCoinRankRepository()

        val viewModel = RankViewModel(repo)

        assertEquals(1, repo.calls)
        viewModel.pagingFlow
    }
}

private class FakeCoinRankRepository : CoinRankRepository {
    var calls = 0

    override fun getCoinRankPagingData(): Flow<PagingData<Coin>> {
        calls++
        return flowOf(PagingData.empty())
    }
}
