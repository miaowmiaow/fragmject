package com.example.fragmject.feature.user.ui.mycoin

import androidx.paging.PagingData
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 我的积分：加载失败降级为默认 Coin，不能让 UI 沿用上一次账号的积分。
 */
class MyCoinViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - loads the user coin`() = runTest {
        val repo = FakeMyRepository(coin = Coin(coinCount = "12", userId = "9"))

        val viewModel = MyCoinViewModel(repo)

        assertEquals("12", viewModel.coin.value.coinCount)
        assertEquals("9", viewModel.coin.value.userId)
    }

    @Test
    fun `init - null coin falls back to defaults`() = runTest {
        val viewModel = MyCoinViewModel(FakeMyRepository(coin = null))

        assertEquals("0", viewModel.coin.value.coinCount)
        assertEquals("---", viewModel.coin.value.nickname)
        assertEquals("0", viewModel.coin.value.userId)
    }
}

private class FakeMyRepository(private val coin: Coin? = null) : MyRepository {
    override suspend fun getUserCoin(): Coin? = coin
    override fun getMyCoinPagingData(): Flow<PagingData<MyCoin>> = emptyFlow()
    override fun getMySharePagingData(): Flow<PagingData<Article>> = emptyFlow()
    override suspend fun shareArticle(title: String, link: String): SimpleResult = SimpleResult(true)
    override suspend fun collectArticle(id: String): SimpleResult = SimpleResult(true)
    override suspend fun uncollectArticle(id: String): SimpleResult = SimpleResult(true)
}
