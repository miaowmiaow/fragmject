package com.example.fragmject.feature.auth.ui.login

import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.usecase.AuthSessionCoordinator
import com.example.fragmject.core.domain.usecase.LoginUseCase
import com.example.fragmject.core.testing.FakeAuthGateway
import com.example.fragmject.core.testing.FakeCookieStore
import com.example.fragmject.core.testing.FakeUserRepository
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [LoginViewModel] 测试：表单校验前置与三态映射。
 *
 * 空用户名/密码必须在 UseCase 层拦下、不触达网关；成功与失败分别映射到
 * [LoginUiState.Success] / [LoginUiState.Error]，`resetMessage` 复位到 Idle。
 */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login with blank username - reports error without hitting gateway`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = LoginViewModel(loginUseCase(gateway))

        viewModel.login("", "pass")

        assertEquals(LoginUiState.Error("用户名不能为空"), viewModel.uiState.value)
        assertTrue(gateway.loginCalls.isEmpty())
    }

    @Test
    fun `login with blank password - reports error without hitting gateway`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = LoginViewModel(loginUseCase(gateway))

        viewModel.login("user", "")

        assertEquals(LoginUiState.Error("密码不能为空"), viewModel.uiState.value)
        assertTrue(gateway.loginCalls.isEmpty())
    }

    @Test
    fun `login success - maps to success state`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = LoginViewModel(loginUseCase(gateway))

        viewModel.login("user", "pass")

        assertEquals(LoginUiState.Success("登录成功"), viewModel.uiState.value)
        assertEquals(listOf("user" to "pass"), gateway.loginCalls)
    }

    @Test
    fun `login failure - maps to error state`() = runTest {
        val gateway = FakeAuthGateway(loginResult = AuthExchange.Failure("密码错误"))
        val viewModel = LoginViewModel(loginUseCase(gateway))

        viewModel.login("user", "wrong")

        assertEquals(LoginUiState.Error("密码错误"), viewModel.uiState.value)
    }

    @Test
    fun `reset message - returns to idle`() = runTest {
        val gateway = FakeAuthGateway(loginResult = AuthExchange.Failure("密码错误"))
        val viewModel = LoginViewModel(loginUseCase(gateway))
        viewModel.login("user", "wrong")

        viewModel.resetMessage()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    private fun loginUseCase(gateway: AuthGateway): LoginUseCase =
        LoginUseCase(gateway, AuthSessionCoordinator(FakeCookieStore(), FakeUserRepository()))
}
