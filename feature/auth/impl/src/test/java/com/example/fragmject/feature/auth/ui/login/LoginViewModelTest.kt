package com.example.fragmject.feature.auth.ui.login

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.domain.usecase.LoginUseCase
import com.example.fragmject.core.model.User
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [LoginViewModel] 测试：表单校验前置与三态映射。
 *
 * 空用户名/密码必须在 UseCase 层拦下、不触达仓储；成功与失败分别映射到
 * [LoginUiState.Success] / [LoginUiState.Error]，`resetMessage` 复位到 Idle。
 */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login with blank username - reports error without hitting repository`() = runTest {
        val repo = FakeUserRepository()
        val viewModel = LoginViewModel(LoginUseCase(repo))

        viewModel.login("", "pass")

        assertEquals(LoginUiState.Error("用户名不能为空"), viewModel.uiState.value)
        assertTrue(repo.loginCalls.isEmpty())
    }

    @Test
    fun `login with blank password - reports error without hitting repository`() = runTest {
        val repo = FakeUserRepository()
        val viewModel = LoginViewModel(LoginUseCase(repo))

        viewModel.login("user", "")

        assertEquals(LoginUiState.Error("密码不能为空"), viewModel.uiState.value)
        assertTrue(repo.loginCalls.isEmpty())
    }

    @Test
    fun `login success - maps to success state`() = runTest {
        val repo = FakeUserRepository(loginResult = AuthResult.Success("登录成功"))
        val viewModel = LoginViewModel(LoginUseCase(repo))

        viewModel.login("user", "pass")

        assertEquals(LoginUiState.Success("登录成功"), viewModel.uiState.value)
        assertEquals(listOf("user" to "pass"), repo.loginCalls)
    }

    @Test
    fun `login failure - maps to error state`() = runTest {
        val repo = FakeUserRepository(loginResult = AuthResult.Error("密码错误"))
        val viewModel = LoginViewModel(LoginUseCase(repo))

        viewModel.login("user", "wrong")

        assertEquals(LoginUiState.Error("密码错误"), viewModel.uiState.value)
    }

    @Test
    fun `reset message - returns to idle`() = runTest {
        val repo = FakeUserRepository(loginResult = AuthResult.Error("密码错误"))
        val viewModel = LoginViewModel(LoginUseCase(repo))
        viewModel.login("user", "wrong")

        viewModel.resetMessage()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }
}

private class FakeUserRepository(
    private val loginResult: AuthResult = AuthResult.Success("ok"),
) : UserRepository {

    val loginCalls = mutableListOf<Pair<String, String>>()

    override suspend fun login(username: String, password: String): AuthResult {
        loginCalls += username to password
        return loginResult
    }

    override suspend fun register(username: String, password: String, repassword: String): AuthResult =
        AuthResult.Error("unused in LoginViewModelTest")

    override suspend fun logout(): Boolean = true

    override suspend fun saveUser(user: User) = Unit

    override fun observeCurrentUser(): Flow<User?> = MutableStateFlow(null)
}
