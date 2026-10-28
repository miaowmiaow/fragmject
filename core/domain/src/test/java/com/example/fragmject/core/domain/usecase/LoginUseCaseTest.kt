package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.testing.FakeAuthGateway
import com.example.fragmject.core.testing.FakeCookieStore
import com.example.fragmject.core.testing.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 登录用例：本地校验必须先于网络请求；合法凭据交给认证网关 + 会话编排。
 */
class LoginUseCaseTest {

    @Test
    fun `blank username returns error without calling gateway`() = runTest {
        val gateway = FakeAuthGateway()

        val result = loginUseCase(gateway)("   ", "password")

        assertEquals(AuthResult.Error("用户名不能为空"), result)
        assertEquals(0, gateway.loginCallCount)
    }

    @Test
    fun `blank password returns error without calling gateway`() = runTest {
        val gateway = FakeAuthGateway()

        val result = loginUseCase(gateway)("alice", "")

        assertEquals(AuthResult.Error("密码不能为空"), result)
        assertEquals(0, gateway.loginCallCount)
    }

    @Test
    fun `valid credentials delegate to gateway and return session result`() = runTest {
        val gateway = FakeAuthGateway()

        val result = loginUseCase(gateway)("alice", "secret")

        assertEquals(AuthResult.Success("登录成功"), result)
        assertEquals(1, gateway.loginCallCount)
        assertEquals(listOf("alice", "secret"), gateway.lastArguments)
    }

    private fun loginUseCase(gateway: FakeAuthGateway): LoginUseCase =
        LoginUseCase(
            gateway = gateway,
            session = AuthSessionCoordinator(
                cookies = FakeCookieStore(),
                users = FakeUserRepository(),
            ),
        )
}
