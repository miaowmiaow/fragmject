package com.example.fragmject.feature.user.ui.user

import androidx.paging.PagingData
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * [UserViewModel] 测试：用户中心信息的加载与重复初始化保护。
 *
 * 重复调用 [UserViewModel.init] 会由 Compose 侧反复触发（重组/返回页面），
 * 必须避免同 userId 重复打网络；userId 变化时则必须重新拉取。
 */
class UserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - loads user coin`() = runTest {
        val repo = FakeUserCenterRepository(coin = Coin(coinCount = "42", nickname = "neo"))
        val viewModel = UserViewModel(repo, FakeCollectState())

        viewModel.init("1")

        assertEquals("42", viewModel.coin.value.coinCount)
        assertEquals("neo", viewModel.coin.value.nickname)
    }

    @Test
    fun `init - missing coin falls back to default`() = runTest {
        val repo = FakeUserCenterRepository(coin = null)
        val viewModel = UserViewModel(repo, FakeCollectState())

        viewModel.init("1")

        assertEquals("---", viewModel.coin.value.nickname)
        assertEquals("0", viewModel.coin.value.coinCount)
    }

    @Test
    fun `init same user twice - loads coin once`() = runTest {
        val repo = FakeUserCenterRepository(coin = Coin(coinCount = "1"))
        val viewModel = UserViewModel(repo, FakeCollectState())

        viewModel.init("1")
        viewModel.init("1")

        assertEquals(listOf("1"), repo.coinRequests)
    }

    @Test
    fun `init different user - reloads coin`() = runTest {
        val repo = FakeUserCenterRepository(coin = Coin(coinCount = "1"))
        val viewModel = UserViewModel(repo, FakeCollectState())

        viewModel.init("1")
        viewModel.init("2")

        assertEquals(listOf("1", "2"), repo.coinRequests)
    }
}

private class FakeUserCenterRepository(
    var coin: Coin? = null,
) : UserCenterRepository {

    val coinRequests = mutableListOf<String>()

    override suspend fun getUserCoin(userId: String): Coin? {
        coinRequests += userId
        return coin
    }

    override fun getUserSharePagingData(userId: String): Flow<PagingData<Article>> = emptyFlow()
}

private class FakeCollectState : CollectState {
    override val overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val collectFailed: Flow<String> = emptyFlow()
    override fun toggle(id: String, collect: Boolean) = Unit
}
