package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.model.User
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * [AuthGatewayImpl]：只验证「响应 → [AuthExchange]」的翻译口径。
 *
 * 会话副作用（Cookie 快照/回滚、落库）已上移到 domain，
 * 对应行为见 `core:domain` 的 `AuthSessionCoordinatorTest`。
 */
class AuthGatewayImplTest {

    @Test
    fun `success response maps user and message`() = runBlocking {
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ ->
                DataResponse(User(id = 1, username = "alice"), "0", "登录成功")
            },
        )

        val result = AuthGatewayImpl(remote).login("alice", "pass")

        assertEquals(AuthExchange.Success(User(id = 1, username = "alice"), "登录成功"), result)
    }

    @Test
    fun `business failure maps message and keeps sessionTouched false`() = runBlocking {
        val remote = FakeUserRemoteDataSource(
            loginResult = { _, _ ->
                // 失败响应即使带 user，也不能当作成功
                DataResponse(User(id = 1, username = "ghost"), "-1", "密码错误")
            },
        )

        val result = AuthGatewayImpl(remote).login("alice", "wrong")

        assertTrue(result is AuthExchange.Failure)
        assertEquals("密码错误", (result as AuthExchange.Failure).message)
        assertEquals(false, result.sessionTouched)
    }

    @Test
    fun `success response without user marks session as touched`() = runBlocking {
        val remote = FakeUserRemoteDataSource(loginResult = { _, _ -> DataResponse(errorCode = "0") })

        val result = AuthGatewayImpl(remote).login("alice", "pass")

        assertEquals(AuthExchange.Failure("用户信息获取失败", sessionTouched = true), result)
    }

    @Test
    fun `logout delegates to remote and propagates failure`() = runBlocking {
        val failure = IOException("offline")
        val remote = FakeUserRemoteDataSource(logoutResult = { throw failure })

        assertEquals(0, remote.logoutCalls)
        try {
            AuthGatewayImpl(remote).logout()
            fail("IOException expected")
        } catch (e: IOException) {
            assertSame(failure, e)
        }
        assertEquals(1, remote.logoutCalls)
    }
}
