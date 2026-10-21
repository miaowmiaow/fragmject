package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.result.AuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 注册用例：本地校验必须先于网络请求，避免无谓往返；合法输入交给网关 + 会话编排。
 */
class RegisterUseCaseTest {

    @Test
    fun `blank username is rejected before calling gateway`() = runTest {
        val gateway = FakeAuthGateway()

        val result = registerUseCase(gateway)("   ", "password", "password")

        assertEquals(AuthResult.Error("用户名不能为空"), result)
        assertEquals(0, gateway.registerCalls)
    }

    @Test
    fun `blank password is rejected before calling gateway`() = runTest {
        val gateway = FakeAuthGateway()

        val result = registerUseCase(gateway)("alice", " ", " ")

        assertEquals(AuthResult.Error("密码不能为空"), result)
        assertEquals(0, gateway.registerCalls)
    }

    @Test
    fun `blank repeated password is rejected before calling gateway`() = runTest {
        val gateway = FakeAuthGateway()

        val result = registerUseCase(gateway)("alice", "password", "  ")

        assertEquals(AuthResult.Error("确认密码不能为空"), result)
        assertEquals(0, gateway.registerCalls)
    }

    @Test
    fun `mismatched passwords are rejected before calling gateway`() = runTest {
        val gateway = FakeAuthGateway()

        val result = registerUseCase(gateway)("alice", "password", "password2")

        assertEquals(AuthResult.Error("两次密码不一样"), result)
        assertEquals(0, gateway.registerCalls)
    }

    @Test
    fun `valid input delegates to gateway`() = runTest {
        val gateway = FakeAuthGateway()

        val result = registerUseCase(gateway)("alice", "password", "password")

        assertEquals(AuthResult.Success("注册成功"), result)
        assertEquals(1, gateway.registerCalls)
        assertEquals(listOf("alice", "password", "password"), gateway.lastArguments)
    }

    @Test
    fun `gateway failure is passed through unchanged`() = runTest {
        val gateway = FakeAuthGateway(registerHandler = { AuthExchange.Failure("用户名已存在") })

        val result = registerUseCase(gateway)("alice", "password", "password")

        assertEquals(AuthResult.Error("用户名已存在"), result)
    }

    private fun registerUseCase(gateway: FakeAuthGateway): RegisterUseCase =
        RegisterUseCase(
            gateway = gateway,
            session = AuthSessionCoordinator(
                cookies = FakeCookieStore(),
                users = FakeUserRepository(),
            ),
        )
}
