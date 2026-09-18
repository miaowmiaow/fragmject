package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.dao.UserDao
import com.example.fragmject.core.database.model.UserEntity
import com.example.fragmject.core.database.store.UserStore
import com.example.fragmject.core.domain.result.LoginResult
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.User
import com.example.fragmject.core.network.datasource.UserRemoteDataSource
import com.example.fragmject.core.network.http.DataResponse
import com.example.fragmject.core.network.http.HttpResponse
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
 * 通过真实 [UserStore]（注入内存 fake [UserDao]）+ fake [UserRemoteDataSource]
 * 覆盖「登录成功写用户、登录失败不写、登出成功清空」的核心契约。
 */
class UserRepositoryImplTest {

    @Test
    fun `login success - persists user`() = runBlocking {
        val dao = FakeUserDao()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ ->
                DataResponse(data = User(id = 1, username = "test"), errorCode = "0", errorMsg = "登录成功")
            }
        )
        val repo = UserRepositoryImpl(remote, UserStore(dao))

        val result = repo.login("test", "pass")

        assertTrue(result is LoginResult.Success)
        assertEquals("test", dao.current()?.username)
    }

    @Test
    fun `login failure - does not persist user`() = runBlocking {
        val dao = FakeUserDao()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ -> DataResponse(errorCode = "-1", errorMsg = "密码错误") }
        )
        val repo = UserRepositoryImpl(remote, UserStore(dao))

        val result = repo.login("test", "wrong")

        assertTrue(result is LoginResult.Error)
        assertNull(dao.current())
    }

    @Test
    fun `logout success - clears user`() = runBlocking {
        val dao = FakeUserDao()
        dao.insert(UserEntity(id = 1, username = "test"))
        val remote = FakeUserRemoteDataSource(
            logoutResult = { HttpResponse(errorCode = "0", errorMsg = "ok") }
        )
        val repo = UserRepositoryImpl(remote, UserStore(dao))

        val result = repo.logout()

        assertTrue(result.success)
        assertNull(dao.current())
    }
}

private class FakeUserDao : UserDao {
    private val userFlow = MutableStateFlow<UserEntity?>(null)

    fun current(): UserEntity? = userFlow.value

    override suspend fun insert(user: UserEntity): Long {
        userFlow.value = user
        return 1L
    }

    override fun get(): Flow<UserEntity?> = userFlow

    override suspend fun clear() {
        userFlow.value = null
    }

    override suspend fun delete(user: UserEntity): Int {
        if (userFlow.value?.id == user.id) {
            userFlow.value = null
            return 1
        }
        return 0
    }
}

private class FakeUserRemoteDataSource(
    private val loginResult: suspend (String, String) -> DataResponse<User> = { _, _ -> DataResponse() },
    private val logoutResult: suspend () -> HttpResponse = { HttpResponse() },
) : UserRemoteDataSource {
    override suspend fun login(username: String, password: String): DataResponse<User> =
        loginResult(username, password)

    override suspend fun register(username: String, password: String, repassword: String): DataResponse<User> =
        DataResponse()

    override suspend fun logout(): HttpResponse = logoutResult()

    override suspend fun getUserShareArticles(userId: String, page: Int): DataResponse<ShareArticle> =
        DataResponse()
}
