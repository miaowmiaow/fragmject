package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.domain.session.SessionCacheCleaner
import com.example.fragmject.core.model.User
import com.example.fragmject.core.testing.FakeCookieStore
import com.example.fragmject.core.testing.FakeUserRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 会话编排的行为基线（原 `UserRepositoryImplTest` 里编排相关用例迁移至此）。
 *
 * 覆盖：快照失败、请求异常/取消后的回滚、业务失败按需清理、落库失败清理，
 * 以及登出的四种异常优先级组合。
 */
class AuthSessionCoordinatorTest {

    @Test
    fun `success persists user and returns response message`() = runBlocking {
        val users = FakeUserRepository()
        val coordinator = AuthSessionCoordinator(FakeCookieStore(), users)

        val result = coordinator.authenticate {
            AuthExchange.Success(User(id = 1, username = "alice"), "登录成功")
        }

        assertEquals(AuthResult.Success("登录成功"), result)
        assertEquals("alice", users.current()?.username)
    }

    @Test
    fun `snapshot read failure returns error without touching session`() = runBlocking {
        val cookies = FakeCookieStore().apply { readFailure = IOException("cookie read failed") }
        val users = FakeUserRepository()
        val coordinator = AuthSessionCoordinator(cookies, users)
        var called = false

        val result = coordinator.authenticate {
            called = true
            AuthExchange.Success(User(id = 1, username = "alice"), "ok")
        }

        assertEquals(AuthResult.Error("登录状态读取失败，请重试"), result)
        assertEquals(false, called)
        assertEquals(0, cookies.clearCalls)
    }

    @Test
    fun `request failure with unchanged cookies keeps existing user`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "existing")) }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        val result = coordinator.authenticate { throw IOException("offline") }

        assertTrue(result is AuthResult.Error)
        assertEquals("existing", users.current()?.username)
        assertEquals(0, cookies.clearCalls)
    }

    @Test
    fun `request failure after cookie mutation clears user and cookies`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "existing")) }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        val result = coordinator.authenticate {
            cookies.session = "session=new"
            throw IOException("failed after cookies")
        }

        assertTrue(result is AuthResult.Error)
        assertNull(users.current())
        assertNull(cookies.session)
    }

    @Test
    fun `request cancellation after cookie mutation clears session and rethrows same exception`() =
        runBlocking {
            val cancellation = CancellationException("cancelled after cookies")
            val users = FakeUserRepository().apply { seed(User(id = 1, username = "existing")) }
            val cookies = FakeCookieStore()
            val coordinator = AuthSessionCoordinator(cookies, users)

            try {
                coordinator.authenticate {
                    cookies.session = "session=new"
                    throw cancellation
                }
                fail("CancellationException expected")
            } catch (e: CancellationException) {
                assertSame(cancellation, e)
            }

            assertNull(users.current())
            assertNull(cookies.session)
        }

    @Test
    fun `failure marked sessionTouched clears session unconditionally`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "existing")) }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        val result = coordinator.authenticate {
            AuthExchange.Failure("用户信息获取失败", sessionTouched = true)
        }

        assertEquals(AuthResult.Error("用户信息获取失败"), result)
        assertNull(users.current())
        assertEquals(1, cookies.clearCalls)
    }

    @Test
    fun `business failure with untouched cookies keeps existing user`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "existing")) }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        val result = coordinator.authenticate { AuthExchange.Failure("密码错误") }

        assertEquals(AuthResult.Error("密码错误"), result)
        assertEquals("existing", users.current()?.username)
        assertEquals(0, cookies.clearCalls)
    }

    @Test
    fun `persistence failure clears session and reports cleanup`() = runBlocking {
        val users = FakeUserRepository().apply { saveFailure = IllegalStateException("db write failed") }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        val result = coordinator.authenticate {
            AuthExchange.Success(User(id = 2, username = "new"), "ok")
        }

        assertEquals(AuthResult.Error("登录状态保存失败，请重试"), result)
        assertNull(users.current())
        assertNull(cookies.session)
    }

    @Test
    fun `persistence cancellation clears session and propagates`() = runBlocking {
        val cancellation = CancellationException("save cancelled")
        val users = FakeUserRepository().apply { saveFailure = cancellation }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        try {
            coordinator.authenticate { AuthExchange.Success(User(id = 1, username = "alice"), "ok") }
            fail("CancellationException expected")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }

        assertNull(users.current())
        assertNull(cookies.session)
    }

    @Test
    fun `logout success clears local session`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "alice")) }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        assertTrue(coordinator.logout { })

        assertNull(users.current())
        assertNull(cookies.session)
    }

    @Test
    fun `logout remote failure still clears local session`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "alice")) }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        assertTrue(coordinator.logout { throw IOException("offline") })

        assertNull(users.current())
        assertNull(cookies.session)
    }

    @Test
    fun `logout cancellation still clears local session and propagates`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "alice")) }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)
        val started = CompletableDeferred<Unit>()
        var job: kotlinx.coroutines.Job? = null

        job = launch {
            coordinator.logout {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(users.current())
        assertNull(cookies.session)
    }

    @Test
    fun `logout with remote and cleanup failure throws cleanup failure`() = runBlocking {
        val cleanupFailure = IOException("local cleanup failed")
        val users = FakeUserRepository().apply { clearFailure = cleanupFailure }
        val cookies = FakeCookieStore()
        val coordinator = AuthSessionCoordinator(cookies, users)

        try {
            coordinator.logout { throw IOException("remote logout failed") }
            fail("Cleanup failure expected")
        } catch (e: IOException) {
            assertSame(cleanupFailure, e)
        }
    }

    @Test
    fun `logout cleanup failure alone keeps success`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "alice")) }
        val cookies = FakeCookieStore().apply { clearFailure = IOException("cookie cleanup failed") }
        val coordinator = AuthSessionCoordinator(cookies, users)

        // 远端已登出：本地清理失败不得把成功翻转成异常，否则会诱导用户重复点击
        assertTrue(coordinator.logout { })
        assertNull("Cookie 清理失败不影响用户表清理", users.current())
        assertEquals(1, cookies.clearCalls)
    }

    @Test
    fun `logout clears session caches after cookies are revoked`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "alice")) }
        val cookies = FakeCookieStore().apply { session = "session=alice" }
        var sessionAtCacheClear: String? = "未调用"
        val caches = FakeSessionCacheCleaner(onClear = { sessionAtCacheClear = cookies.session })
        val coordinator = AuthSessionCoordinator(cookies, users, caches)

        assertTrue(coordinator.logout { })

        assertEquals(1, caches.clearCalls)
        // 顺序不变式：缓存清理必须发生在 Cookie 撤销之后，
        // 否则清理窗口内可能又发起带 Cookie 的请求，把带身份的响应写回磁盘
        assertNull(sessionAtCacheClear)
    }

    @Test
    fun `session cache cleanup failure alone keeps logout success`() = runBlocking {
        val users = FakeUserRepository().apply { seed(User(id = 1, username = "alice")) }
        val cookies = FakeCookieStore()
        val caches = FakeSessionCacheCleaner().apply { failure = IOException("cache cleanup failed") }
        val coordinator = AuthSessionCoordinator(cookies, users, caches)

        // 与 Cookie 清理失败同一口径：远端已登出时不翻转成异常
        assertTrue(coordinator.logout { })
        assertEquals(1, caches.clearCalls)
        assertNull(users.current())
    }

    @Test
    fun `authentication failure with touched session clears session caches`() = runBlocking {
        val users = FakeUserRepository()
        val cookies = FakeCookieStore()
        val caches = FakeSessionCacheCleaner()
        val coordinator = AuthSessionCoordinator(cookies, users, caches)

        val result = coordinator.authenticate {
            AuthExchange.Failure("用户信息获取失败", sessionTouched = true)
        }

        assertEquals(AuthResult.Error("用户信息获取失败"), result)
        assertEquals(1, caches.clearCalls)
    }

    @Test
    fun `successful authentication keeps session caches`() = runBlocking {
        val users = FakeUserRepository()
        val caches = FakeSessionCacheCleaner()
        val coordinator = AuthSessionCoordinator(FakeCookieStore(), users, caches)

        coordinator.authenticate { AuthExchange.Success(User(id = 1, username = "alice"), "登录成功") }

        assertEquals(0, caches.clearCalls)
    }
}

/** 会话缓存清理替身：记录调用次数，可注入失败，并可在清理时观测外部状态以验证顺序。 */
private class FakeSessionCacheCleaner(
    private val onClear: (() -> Unit)? = null,
) : SessionCacheCleaner {

    var clearCalls = 0
    var failure: Exception? = null

    override suspend fun clearSessionCaches() {
        // 与 FakeCookieStore 一致：取消必须先传播
        currentCoroutineContext().ensureActive()
        clearCalls++
        onClear?.invoke()
        failure?.let { throw it }
    }
}
