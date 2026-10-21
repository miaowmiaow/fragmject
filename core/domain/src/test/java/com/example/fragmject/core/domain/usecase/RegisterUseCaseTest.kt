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
 * 注册流程：本地校验必须先于网络请求，避免无谓往返；合法输入透传仓库结果。
 */
class RegisterUseCaseTest {

    @Test
    fun `blank username is rejected before calling the repository`() = runTest {
        val repo = FakeRegisterUserRepository()

        val result = RegisterUseCase(repo)("   ", "password", "password")

        assertEquals(AuthResult.Error("用户名不能为空"), result)
        assertEquals(0, repo.registerCalls)
    }

    @Test
    fun `blank password is rejected before calling the repository`() = runTest {
        val repo = FakeRegisterUserRepository()

        val result = RegisterUseCase(repo)("alice", " ", " ")

        assertEquals(AuthResult.Error("密码不能为空"), result)
        assertEquals(0, repo.registerCalls)
    }

    @Test
    fun `blank repeated password is rejected before calling the repository`() = runTest {
        val repo = FakeRegisterUserRepository()

        val result = RegisterUseCase(repo)("alice", "password", "  ")

        assertEquals(AuthResult.Error("确认密码不能为空"), result)
        assertEquals(0, repo.registerCalls)
    }

    @Test
    fun `mismatched passwords are rejected before calling the repository`() = runTest {
        val repo = FakeRegisterUserRepository()

        val result = RegisterUseCase(repo)("alice", "password", "password2")

        assertEquals(AuthResult.Error("两次密码不一样"), result)
        assertEquals(0, repo.registerCalls)
    }

    @Test
    fun `valid input delegates to the repository`() = runTest {
        val repo = FakeRegisterUserRepository(result = AuthResult.Success("注册成功"))

        val result = RegisterUseCase(repo)("alice", "password", "password")

        assertEquals(AuthResult.Success("注册成功"), result)
        assertEquals(1, repo.registerCalls)
        assertEquals(listOf("alice", "password", "password"), repo.lastArguments)
    }

    @Test
    fun `repository error is passed through unchanged`() = runTest {
        val repo = FakeRegisterUserRepository(result = AuthResult.Error("用户名已存在"))

        val result = RegisterUseCase(repo)("alice", "password", "password")

        assertEquals(AuthResult.Error("用户名已存在"), result)
    }
}

private class FakeRegisterUserRepository(
    private val result: AuthResult = AuthResult.Success("ok"),
) : UserRepository {
    var registerCalls = 0
    var lastArguments: List<String> = emptyList()

    override suspend fun login(username: String, password: String): AuthResult = AuthResult.Error("未实现")

    override suspend fun register(username: String, password: String, repassword: String): AuthResult {
        registerCalls++
        lastArguments = listOf(username, password, repassword)
        return result
    }

    override suspend fun logout(): Boolean = false
    override suspend fun saveUser(user: User) = Unit
    override fun observeCurrentUser(): Flow<User?> = flowOf(null)
}
