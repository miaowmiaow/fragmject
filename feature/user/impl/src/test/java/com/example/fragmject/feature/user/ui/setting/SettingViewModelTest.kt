package com.example.fragmject.feature.user.ui.setting

import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.domain.system.SystemStorage
import com.example.fragmject.core.domain.usecase.AuthSessionCoordinator
import com.example.fragmject.core.domain.usecase.LogoutUseCase
import com.example.fragmject.core.testing.FakeAuthGateway
import com.example.fragmject.core.testing.FakeCookieStore
import com.example.fragmject.core.testing.FakeUserRepository
import com.example.fragmject.core.testing.MainDispatcherRule
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * [SettingViewModel] 测试：登出与清缓存的事件出口。
 *
 * 登出编排上移到 domain 后，ViewModel 只需区分两种结果：
 * - 用例返回 true（含「远端失败但本地已清理」）→ [SettingEvent.LogoutSucceeded]；
 * - 用例抛异常（远端与本地清理都失败）→ [SettingEvent.LogoutFailed]。
 * 两条都**必须退出 Loading 态**，否则设置页会永久转圈。
 */
class SettingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `logout success - emits LogoutSucceeded and leaves loading`() = runTest {
        val viewModel = createViewModel()

        viewModel.logout()

        assertEquals(SettingEvent.LogoutSucceeded, viewModel.events.first())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `logout remote failure with local cleanup - still reports success`() = runTest {
        val gateway = FakeAuthGateway().apply { logoutFailure = IOException("offline") }
        val viewModel = createViewModel(gateway = gateway)

        viewModel.logout()

        assertEquals(SettingEvent.LogoutSucceeded, viewModel.events.first())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `logout exception - emits LogoutFailed and restores state`() = runTest {
        val gateway = FakeAuthGateway().apply { logoutFailure = IOException("offline") }
        val cookies = FakeCookieStore().apply { clearFailure = IOException("local cleanup failed") }
        val viewModel = createViewModel(gateway = gateway, cookies = cookies)

        viewModel.logout()

        assertEquals(SettingEvent.LogoutFailed, viewModel.events.first())
        assertFalse("异常后必须退出 Loading，否则设置页永久转圈", viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clear cache failure - emits CacheClearFailed with storage message`() = runTest {
        val storage = FakeSystemStorage(clearResult = SimpleResult(success = false, message = "缓存目录不可写"))
        val viewModel = createViewModel(storage = storage)

        viewModel.clearCache()

        assertEquals(SettingEvent.CacheClearFailed("缓存目录不可写"), viewModel.events.first())
    }

    private fun createViewModel(
        gateway: FakeAuthGateway = FakeAuthGateway(),
        cookies: FakeCookieStore = FakeCookieStore(),
        storage: SystemStorage = FakeSystemStorage(),
    ): SettingViewModel = SettingViewModel(
        userRepo = FakeUserRepository(),
        logoutUseCase = LogoutUseCase(gateway, AuthSessionCoordinator(cookies, FakeUserRepository())),
        themeRepo = FakeThemeRepository(),
        systemStorage = storage,
    )
}

private class FakeThemeRepository : ThemeRepository {
    private val darkTheme = MutableStateFlow(false)
    override fun observeDarkTheme(): Flow<Boolean> = darkTheme
    override suspend fun setDarkTheme(dark: Boolean) {
        darkTheme.value = dark
    }
}

private class FakeSystemStorage(
    private val clearResult: SimpleResult = SimpleResult(success = true),
) : SystemStorage {

    override val cacheSizeText = MutableStateFlow("0B")
    override val isClearing = MutableStateFlow(false)

    override fun cacheDirectory(): File = File("build/tmp/setting-test-cache")

    override suspend fun refreshCacheSize() = Unit

    override suspend fun clearCache(): SimpleResult = clearResult
}
