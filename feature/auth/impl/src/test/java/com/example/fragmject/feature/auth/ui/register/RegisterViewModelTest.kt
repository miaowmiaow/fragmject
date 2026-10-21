package com.example.fragmject.feature.auth.ui.register

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.domain.usecase.RegisterUseCase
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
 * [RegisterViewModel] 测试：三项表单校验与结果映射。
 *
 * 两次密码不一致是最容易漏的校验分支，且必须在 UseCase 层拦下、不触达仓储。
 */
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `register with blank username - reports error without hitting repository`() = runTest {
        val repo = FakeUserRepository()
        val viewModel = RegisterViewModel(RegisterUseCase(repo))

        viewModel.register("", "pass", "pass")

        assertEquals(RegisterUiState.Error("用户名不能为空"), viewModel.uiState.value)
        assertTrue(repo.registerCalls.isEmpty())
    }

    @Test
    fun `register with mismatched passwords - reports error without hitting repository`() = runTest {
        val repo = FakeUserRepository()
        val viewModel = RegisterViewModel(RegisterUseCase(repo))

        viewModel.register("user", "pass", "other")

        assertEquals(RegisterUiState.Error("两次密码不一样"), viewModel.uiState.value)
        assertTrue(repo.registerCalls.isEmpty())
    }

    @Test
    fun `register success - maps to success state`() = runTest {
        val repo = FakeUserRepository(registerResult = AuthResult.Success("注册成功"))
        val viewModel = RegisterViewModel(RegisterUseCase(repo))

        viewModel.register("user", "pass", "pass")

        assertEquals(RegisterUiState.Success("注册成功"), viewModel.uiState.value)
        assertEquals(listOf(Triple("user", "pass", "pass")), repo.registerCalls)
    }

    @Test
    fun `register failure - maps to error state`() = runTest {
        val repo = FakeUserRepository(registerResult = AuthResult.Error("用户名已存在"))
        val viewModel = RegisterViewModel(RegisterUseCase(repo))

        viewModel.register("user", "pass", "pass")

        assertEquals(RegisterUiState.Error("用户名已存在"), viewModel.uiState.value)
    }

    @Test
    fun `reset message - returns to idle`() = runTest {
        val repo = FakeUserRepository(registerResult = AuthResult.Error("用户名已存在"))
        val viewModel = RegisterViewModel(RegisterUseCase(repo))
        viewModel.register("user", "pass", "pass")

        viewModel.resetMessage()

        assertEquals(RegisterUiState.Idle, viewModel.uiState.value)
    }
}

private class FakeUserRepository(
    private val registerResult: AuthResult = AuthResult.Success("ok"),
) : UserRepository {

    val registerCalls = mutableListOf<Triple<String, String, String>>()

    override suspend fun login(username: String, password: String): AuthResult =
        AuthResult.Error("unused in RegisterViewModelTest")

    override suspend fun register(username: String, password: String, repassword: String): AuthResult {
        registerCalls += Triple(username, password, repassword)
        return registerResult
    }

    override suspend fun logout(): Boolean = true

    override suspend fun saveUser(user: User) = Unit

    override fun observeCurrentUser(): Flow<User?> = MutableStateFlow(null)
}
