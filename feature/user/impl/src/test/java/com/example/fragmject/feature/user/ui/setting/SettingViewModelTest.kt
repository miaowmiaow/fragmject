package com.example.fragmject.feature.user.ui.setting

import com.example.fragmject.core.domain.repository.ThemeRepository
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.domain.system.SystemStorage
import com.example.fragmject.core.model.User
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
 * 登出有三条分支必须区分：成功 / 仓储返回 false / 仓储抛异常。
 * 后两条都要落到 [SettingEvent.LogoutFailed]，且**必须退出 Loading 态**，
 * 否则设置页会永久转圈（这是纯 UI 断言覆盖不到的 ViewModel 责任）。
 */
class SettingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `logout success - emits LogoutSucceeded and leaves loading`() = runTest {
        val viewModel = createViewModel(FakeUserRepository(logoutResult = { true }))

        viewModel.logout()

        assertEquals(SettingEvent.LogoutSucceeded, viewModel.events.first())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `logout rejected by repository - emits LogoutFailed`() = runTest {
        val viewModel = createViewModel(FakeUserRepository(logoutResult = { false }))

        viewModel.logout()

        assertEquals(SettingEvent.LogoutFailed, viewModel.events.first())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `logout exception - emits LogoutFailed and restores state`() = runTest {
        val viewModel = createViewModel(
            FakeUserRepository(logoutResult = { throw IOException("local cleanup failed") }),
        )

        viewModel.logout()

        assertEquals(SettingEvent.LogoutFailed, viewModel.events.first())
        assertFalse("异常后必须退出 Loading，否则设置页永久转圈", viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clear cache failure - emits CacheClearFailed with storage message`() = runTest {
        val storage = FakeSystemStorage(clearResult = SimpleResult(success = false, message = "缓存目录不可写"))
        val viewModel = createViewModel(FakeUserRepository(), storage)

        viewModel.clearCache()

        assertEquals(SettingEvent.CacheClearFailed("缓存目录不可写"), viewModel.events.first())
    }

    private fun createViewModel(
        userRepo: UserRepository,
        storage: SystemStorage = FakeSystemStorage(),
    ): SettingViewModel = SettingViewModel(userRepo, FakeThemeRepository(), storage)
}

private class FakeUserRepository(
    private val logoutResult: suspend () -> Boolean = { true },
) : UserRepository {

    override suspend fun login(username: String, password: String): AuthResult =
        AuthResult.Error("unused in this test")

    override suspend fun register(username: String, password: String, repassword: String): AuthResult =
        AuthResult.Error("unused in this test")

    override suspend fun logout(): Boolean = logoutResult()

    override suspend fun saveUser(user: User) = Unit

    override fun observeCurrentUser(): Flow<User?> = MutableStateFlow(null)
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
