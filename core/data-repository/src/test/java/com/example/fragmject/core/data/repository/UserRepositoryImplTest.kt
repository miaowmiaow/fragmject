package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.result.LoginResult
import com.example.fragmject.core.domain.result.RegisterResult
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [UserRepositoryImpl] 测试：验证登录/登出的持久化副作用。
 *
 * 通过 fake [UserLocalDataSource] + fake [UserRemoteDataSource]
 * 覆盖「登录成功写用户、登录失败不写、登出成功清空」的核心契约。
 */
class UserRepositoryImplTest {

    @Test
    fun `login success - persists user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ ->
                DataResponse(data = User(id = 1, username = "test"), errorCode = "0", errorMsg = "登录成功")
            }
        )
        val repo = UserRepositoryImpl(remote, local)

        val result = repo.login("test", "pass")

        assertTrue(result is LoginResult.Success)
        assertEquals("test", local.current()?.username)
    }

    @Test
    fun `login failure - does not persist user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ -> DataResponse(errorCode = "-1", errorMsg = "密码错误") }
        )
        val repo = UserRepositoryImpl(remote, local)

        val result = repo.login("test", "wrong")

        assertTrue(result is LoginResult.Error)
        assertNull(local.current())
    }

    @Test
    fun `login failure with data - does not persist user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ ->
                DataResponse(data = User(id = 1, username = "ghost"), errorCode = "-1", errorMsg = "失败却带数据")
            }
        )
        val repo = UserRepositoryImpl(remote, local)

        val result = repo.login("test", "wrong")

        assertTrue(result is LoginResult.Error)
        assertNull(local.current())
    }

    @Test
    fun `register success - persists user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(
            registerResult = { _, _, _ ->
                DataResponse(data = User(id = 2, username = "new"), errorCode = "0", errorMsg = "注册成功")
            }
        )
        val repo = UserRepositoryImpl(remote, local)

        val result = repo.register("new", "pass", "pass")

        assertTrue(result is RegisterResult.Success)
        assertEquals("new", local.current()?.username)
    }

    @Test
    fun `register failure with data - does not persist user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(
            registerResult = { _, _, _ ->
                DataResponse(data = User(id = 2, username = "ghost"), errorCode = "-1", errorMsg = "失败却带数据")
            }
        )
        val repo = UserRepositoryImpl(remote, local)

        val result = repo.register("new", "pass", "pass")

        assertTrue(result is RegisterResult.Error)
        assertNull(local.current())
    }

    @Test
    fun `logout success - clears user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "test"))
        val remote = FakeUserRemoteDataSource(
            logoutResult = { HttpResponse(errorCode = "0", errorMsg = "ok") }
        )
        val repo = UserRepositoryImpl(remote, local)

        val result = repo.logout()

        assertTrue(result)
        assertNull(local.current())
    }
}

private class FakeUserLocalDataSource : UserLocalDataSource {
    private val userFlow = MutableStateFlow<User?>(null)

    fun current(): User? = userFlow.value

    override fun observeCurrentUser(): Flow<User?> = userFlow
    override suspend fun saveUser(user: User) { userFlow.value = user }
    override suspend fun clearUser() { userFlow.value = null }
}

private class FakeUserRemoteDataSource(
    private val loginResult: suspend (String, String) -> DataResponse<User> = { _, _ -> DataResponse() },
    private val registerResult: suspend (String, String, String) -> DataResponse<User> = { _, _, _ -> DataResponse() },
    private val logoutResult: suspend () -> HttpResponse = { HttpResponse() },
) : UserRemoteDataSource {
    override suspend fun login(username: String, password: String): DataResponse<User> =
        loginResult(username, password)

    override suspend fun register(username: String, password: String, repassword: String): DataResponse<User> =
        registerResult(username, password, repassword)

    override suspend fun logout(): HttpResponse = logoutResult()

    override suspend fun getUserShareArticles(userId: String, page: Int): DataResponse<ShareArticle> =
        DataResponse()
}
