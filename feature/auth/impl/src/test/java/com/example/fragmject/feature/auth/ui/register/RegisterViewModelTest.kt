package com.example.fragmject.feature.auth.ui.register

import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.usecase.AuthSessionCoordinator
import com.example.fragmject.core.domain.usecase.RegisterUseCase
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
 * [RegisterViewModel] 测试：本地校验不触达网关、结果三态映射。
 */
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `register with blank username - reports error without hitting gateway`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = RegisterViewModel(registerUseCase(gateway))

        viewModel.register("", "pass", "pass")

        assertEquals(RegisterUiState.Error("用户名不能为空"), viewModel.uiState.value)
        assertTrue(gateway.registerCalls.isEmpty())
    }

    @Test
    fun `register with mismatched passwords - reports error without hitting gateway`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = RegisterViewModel(registerUseCase(gateway))

        viewModel.register("user", "pass", "other")

        assertEquals(RegisterUiState.Error("两次密码不一样"), viewModel.uiState.value)
        assertTrue(gateway.registerCalls.isEmpty())
    }

    @Test
    fun `register success - maps to success state`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = RegisterViewModel(registerUseCase(gateway))

        viewModel.register("user", "pass", "pass")

        assertEquals(RegisterUiState.Success("注册成功"), viewModel.uiState.value)
        assertEquals(listOf(Triple("user", "pass", "pass")), gateway.registerCalls)
    }

    @Test
    fun `register failure - maps to error state`() = runTest {
        val gateway = FakeAuthGateway(registerResult = AuthExchange.Failure("用户名已存在"))
        val viewModel = RegisterViewModel(registerUseCase(gateway))

        viewModel.register("user", "pass", "pass")

        assertEquals(RegisterUiState.Error("用户名已存在"), viewModel.uiState.value)
    }

    @Test
    fun `reset message - returns to idle`() = runTest {
        val gateway = FakeAuthGateway(registerResult = AuthExchange.Failure("用户名已存在"))
        val viewModel = RegisterViewModel(registerUseCase(gateway))
        viewModel.register("user", "pass", "pass")

        viewModel.resetMessage()

        assertEquals(RegisterUiState.Idle, viewModel.uiState.value)
    }

    private fun registerUseCase(gateway: AuthGateway): RegisterUseCase =
        RegisterUseCase(gateway, AuthSessionCoordinator(FakeCookieStore(), FakeUserRepository()))
}
