package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.LoginResult
import com.example.fragmject.core.domain.result.LogoutResult
import com.example.fragmject.core.domain.result.RegisterResult
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 登录流程冒烟测试（阶段 0 产出物）。
 *
 * 验证 [LoginUseCase] 的输入校验与领域端口委托：
 * 1. 空用户名/密码 → 校验错误；
 * 2. 合法凭据 → 委托 [UserRepository.login] 并透传结果。
 * 这是阶段 3「认证领域收敛」的行为基线。
 */
class LoginUseCaseTest {

    @Test
    fun `blank username returns error`() = runTest {
        val useCase = LoginUseCase(FakeUserRepository())
        val result = useCase("   ", "password")
        assertEquals(LoginResult.Error("用户名不能为空"), result)
    }

    @Test
    fun `blank password returns error`() = runTest {
        val useCase = LoginUseCase(FakeUserRepository())
        val result = useCase("alice", "")
        assertEquals(LoginResult.Error("密码不能为空"), result)
    }

    @Test
    fun `valid credentials delegates to repository`() = runTest {
        val repo = FakeUserRepository(loginResult = LoginResult.Success("登录成功"))
        val useCase = LoginUseCase(repo)
        val result = useCase("alice", "secret")
        assertEquals(LoginResult.Success("登录成功"), result)
    }
}

private class FakeUserRepository(
    private val loginResult: LoginResult = LoginResult.Error("未实现"),
) : UserRepository {
    override suspend fun login(username: String, password: String): LoginResult = loginResult
    override suspend fun register(username: String, password: String, repassword: String): RegisterResult =
        RegisterResult.Error("未实现")
    override suspend fun logout(): LogoutResult = LogoutResult(success = false)
    override suspend fun saveUser(user: User) = Unit
    override fun observeCurrentUser(): Flow<User?> = flowOf(null)
}
