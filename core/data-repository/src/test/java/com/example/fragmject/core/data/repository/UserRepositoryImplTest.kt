package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.User
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * [UserRepositoryImpl] 测试：验证登录/登出的持久化副作用。
 *
 * 通过 fake [UserLocalDataSource] + fake [UserRemoteDataSource]
 * 覆盖认证成功持久化、预期异常、取消传播及远端退出失败仍清理本地会话。
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
        val repo = UserRepositoryImpl(remote, local, FakeSessionCookieStore())

        val result = repo.login("test", "pass")

        assertTrue(result is AuthResult.Success)
        assertEquals("test", local.current()?.username)
    }

    @Test
    fun `login failure - does not persist user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ -> DataResponse(errorCode = "-1", errorMsg = "密码错误") }
        )
        val repo = UserRepositoryImpl(remote, local, FakeSessionCookieStore())

        val result = repo.login("test", "wrong")

        assertTrue(result is AuthResult.Error)
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
        val repo = UserRepositoryImpl(remote, local, FakeSessionCookieStore())

        val result = repo.login("test", "wrong")

        assertTrue(result is AuthResult.Error)
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
        val repo = UserRepositoryImpl(remote, local, FakeSessionCookieStore())

        val result = repo.register("new", "pass", "pass")

        assertTrue(result is AuthResult.Success)
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
        val repo = UserRepositoryImpl(remote, local, FakeSessionCookieStore())

        val result = repo.register("new", "pass", "pass")

        assertTrue(result is AuthResult.Error)
        assertNull(local.current())
    }

    @Test
    fun `logout success - clears user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "test"))
        val remote = FakeUserRemoteDataSource(
            logoutResult = { HttpResponse(errorCode = "0", errorMsg = "ok") }
        )
        val repo = UserRepositoryImpl(remote, local, FakeSessionCookieStore())

        val result = repo.logout()

        assertTrue(result)
        assertNull(local.current())
    }

    @Test
    fun `login network failure - returns error without changing existing user`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "existing"))
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(loginResult = { _, _ -> throw IOException("offline") })

        val result = UserRepositoryImpl(remote, local, cookies).login("test", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals("existing", local.current()?.username)
        assertEquals(0, cookies.clearCalls)
    }

    @Test
    fun `register remote exception - returns error`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(
            registerResult = { _, _, _ -> throw IOException("HTTP request failed") },
        )

        val result = UserRepositoryImpl(remote, local, FakeSessionCookieStore()).register("new", "pass", "pass")

        assertTrue(result is AuthResult.Error)
        assertNull(local.current())
    }

    @Test
    fun `login persistence failure - invalidates user and cookies`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "existing"))
        local.saveFailure = IllegalStateException("database write failed")
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ -> DataResponse(User(id = 2, username = "new"), "0", "ok") },
        )

        val result = UserRepositoryImpl(remote, local, cookies).login("new", "pass")

        assertTrue(result is AuthResult.Error)
        assertNull(local.current())
        assertNull(cookies.session)
        assertEquals(1, cookies.flushCalls)
    }

    @Test
    fun `register persistence failure - invalidates user and cookies`() = runBlocking {
        val local = FakeUserLocalDataSource().apply { saveFailure = IOException("disk full") }
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(
            registerResult = { _, _, _ -> DataResponse(User(id = 2, username = "new"), "0", "ok") },
        )

        val result = UserRepositoryImpl(remote, local, cookies).register("new", "pass", "pass")

        assertTrue(result is AuthResult.Error)
        assertNull(local.current())
        assertNull(cookies.session)
    }

    @Test
    fun `successful response without user - invalidates uncommitted session`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "existing"))
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(loginResult = { _, _ -> DataResponse(errorCode = "0") })

        val result = UserRepositoryImpl(remote, local, cookies).login("test", "pass")

        assertTrue(result is AuthResult.Error)
        assertNull(local.current())
        assertNull(cookies.session)
    }

    @Test
    fun `login remote cancellation - propagates unchanged`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val local = FakeUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(loginResult = { _, _ -> throw cancellation })
        try {
            UserRepositoryImpl(remote, local, FakeSessionCookieStore()).login("test", "pass")
            fail("CancellationException expected")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun `register remote cancellation - propagates unchanged`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val remote = FakeUserRemoteDataSource(registerResult = { _, _, _ -> throw cancellation })
        try {
            UserRepositoryImpl(remote, FakeUserLocalDataSource(), FakeSessionCookieStore())
                .register("new", "pass", "pass")
            fail("CancellationException expected")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun `persistence cancellation - propagates and invalidates session`() = runBlocking {
        val cancellation = CancellationException("save cancelled")
        val local = FakeUserLocalDataSource().apply { saveFailure = cancellation }
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ -> DataResponse(User(id = 1, username = "test"), "0", "ok") },
        )
        try {
            UserRepositoryImpl(remote, local, cookies).login("test", "pass")
            fail("CancellationException expected")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
        assertNull(local.current())
        assertNull(cookies.session)
    }

    @Test
    fun `logout rejected remotely - invalidates local session and returns true`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "test"))
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(logoutResult = { HttpResponse(errorCode = "-1") })

        val result = UserRepositoryImpl(remote, local, cookies).logout()

        assertTrue(result)
        assertNull(local.current())
        assertNull(cookies.session)
        assertEquals(1, cookies.flushCalls)
    }

    @Test
    fun `logout network failure - invalidates local session and returns true`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "test"))
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(logoutResult = { throw IOException("offline") })

        val result = UserRepositoryImpl(remote, local, cookies).logout()

        assertTrue(result)
        assertNull(local.current())
        assertNull(cookies.session)
    }

    @Test
    fun `logout cancelled in flight - still invalidates local session`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "test"))
        val cookies = FakeSessionCookieStore()
        val started = CompletableDeferred<Unit>()
        val remote = FakeUserRemoteDataSource(logoutResult = {
            started.complete(Unit)
            awaitCancellation()
        })
        val repo = UserRepositoryImpl(remote, local, cookies)
        val job = launch { repo.logout() }
        started.await()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(local.current())
        assertNull(cookies.session)
        assertEquals(1, local.clearCalls)
    }

    @Test
    fun `logout cookie cleanup failure - still clears user and keeps remote success`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "test"))
        val cookies = FakeSessionCookieStore().apply { clearFailure = IOException("cookie cleanup failed") }
        val remote = FakeUserRemoteDataSource(logoutResult = { HttpResponse(errorCode = "0") })

        // 远端登出已生效：本地清理失败不得把「成功」翻转成异常，否则 UI 会提示登出失败、
        // 诱导用户重复点击（远端会话此时已不存在）。残留数据由下次启动的会话校验兜底。
        assertTrue(UserRepositoryImpl(remote, local, cookies).logout())

        assertNull("Cookie 清理失败不影响数据库清理", local.current())
        assertEquals(1, cookies.clearCalls)
    }

    @Test
    fun `logout database cleanup failure - still clears cookies and keeps remote success`() = runBlocking {
        val local = FakeUserLocalDataSource().apply { clearFailure = IOException("database cleanup failed") }
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(logoutResult = { HttpResponse(errorCode = "0") })

        assertTrue(UserRepositoryImpl(remote, local, cookies).logout())

        assertNull("数据库清理失败不影响 Cookie 清理", cookies.session)
        assertEquals(1, cookies.flushCalls)
    }

    @Test
    fun `logout remote failure with cleanup failure - propagates the cleanup failure`() = runBlocking {
        val cleanupFailure = IOException("local cleanup failed")
        val local = FakeUserLocalDataSource().apply { clearFailure = cleanupFailure }
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(logoutResult = { throw IOException("remote logout failed") })

        // 远端已失败时，本地清理失败必须抛出，让调用方知道会话可能残留
        try {
            UserRepositoryImpl(remote, local, cookies).logout()
            fail("Cleanup failure expected")
        } catch (e: IOException) {
            assertSame(cleanupFailure, e)
        }
        assertNull(cookies.session)
    }

    @Test
    fun `login network failure after cookie mutation - clears inconsistent old account`() = runBlocking {
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "existing"))
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(loginResult = { _, _ ->
            cookies.session = "session=new"
            throw IOException("response failed after cookies")
        })

        val result = UserRepositoryImpl(remote, local, cookies).login("new", "pass")

        assertTrue(result is AuthResult.Error)
        assertNull(local.current())
        assertNull(cookies.session)
        assertEquals(1, cookies.flushCalls)
    }

    @Test
    fun `login cancellation after cookie mutation - clears session and preserves cancellation`() = runBlocking {
        val cancellation = CancellationException("cancelled after cookies")
        val local = FakeUserLocalDataSource()
        local.saveUser(User(id = 1, username = "existing"))
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(loginResult = { _, _ ->
            cookies.session = "session=new"
            throw cancellation
        })
        try {
            UserRepositoryImpl(remote, local, cookies).login("new", "pass")
            fail("CancellationException expected")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
        assertNull(local.current())
        assertNull(cookies.session)
    }

    @Test
    fun `register business failure after cookie mutation - clears inconsistent session`() = runBlocking {
        val local = FakeUserLocalDataSource()
        val cookies = FakeSessionCookieStore()
        val remote = FakeUserRemoteDataSource(registerResult = { _, _, _ ->
            cookies.session = "session=new"
            DataResponse(errorCode = "-1", errorMsg = "rejected")
        })

        val result = UserRepositoryImpl(remote, local, cookies).register("new", "pass", "pass")

        assertTrue(result is AuthResult.Error)
        assertNull(local.current())
        assertNull(cookies.session)
    }
}

private class FakeUserLocalDataSource : UserLocalDataSource {
    private val userFlow = MutableStateFlow<User?>(null)
    var saveFailure: Exception? = null
    var clearFailure: Exception? = null
    var clearCalls = 0

    fun current(): User? = userFlow.value

    override fun observeCurrentUser(): Flow<User?> = userFlow
    override suspend fun saveUser(user: User) {
        saveFailure?.let { throw it }
        userFlow.value = user
    }
    override suspend fun clearUser() {
        currentCoroutineContext().ensureActive()
        clearCalls++
        clearFailure?.let { throw it }
        userFlow.value = null
    }
}

private class FakeSessionCookieStore : CookieStore {
    var session: String? = "session=active"
    var clearCalls = 0
    var flushCalls = 0
    var clearFailure: Exception? = null

    override fun getCookie(url: String): String? = session
    override fun setCookie(url: String, value: String) { session = value }
    override fun flush() { flushCalls++ }
    override suspend fun clearSession() {
        currentCoroutineContext().ensureActive()
        clearCalls++
        clearFailure?.let { throw it }
        session = null
        flush()
    }
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
