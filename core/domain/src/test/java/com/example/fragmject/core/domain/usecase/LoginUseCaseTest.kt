package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
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
        assertEquals(AuthResult.Error("用户名不能为空"), result)
    }

    @Test
    fun `blank password returns error`() = runTest {
        val useCase = LoginUseCase(FakeUserRepository())
        val result = useCase("alice", "")
        assertEquals(AuthResult.Error("密码不能为空"), result)
    }

    @Test
    fun `valid credentials delegates to repository`() = runTest {
        val repo = FakeUserRepository(loginResult = AuthResult.Success("登录成功"))
        val useCase = LoginUseCase(repo)
        val result = useCase("alice", "secret")
        assertEquals(AuthResult.Success("登录成功"), result)
    }
}

private class FakeUserRepository(
    private val loginResult: AuthResult = AuthResult.Error("未实现"),
) : UserRepository {
    override suspend fun login(username: String, password: String): AuthResult = loginResult
    override suspend fun register(username: String, password: String, repassword: String): AuthResult =
        AuthResult.Error("未实现")
    override suspend fun logout(): Boolean = false
    override suspend fun saveUser(user: User) = Unit
    override fun observeCurrentUser(): Flow<User?> = flowOf(null)
}
