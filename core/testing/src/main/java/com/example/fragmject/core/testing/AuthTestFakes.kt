package com.example.fragmject.core.testing

import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 认证相关的共享测试替身。
 *
 * 会话编排上移到 domain 后，各 feature 的 ViewModel 测试需要用
 * `AuthGateway + AuthSessionCoordinator + UserRepository` 组装用例；
 * 把这些 fake 收在 `core:testing` 里，避免每个 feature 各抄一份。
 */
class FakeAuthGateway(
    var loginResult: AuthExchange = AuthExchange.Success(User(id = 1, username = "alice"), "登录成功"),
    var registerResult: AuthExchange = AuthExchange.Success(User(id = 2, username = "bob"), "注册成功"),
) : AuthGateway {

    val loginCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<Triple<String, String, String>>()
    var logoutCalls = 0
    var logoutFailure: Exception? = null

    override suspend fun login(username: String, password: String): AuthExchange {
        loginCalls += username to password
        return loginResult
    }

    override suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): AuthExchange {
        registerCalls += Triple(username, password, repassword)
        return registerResult
    }

    override suspend fun logout() {
        logoutCalls++
        logoutFailure?.let { throw it }
    }
}

class FakeCookieStore(
    var session: String? = "session=active",
) : CookieStore {

    var clearCalls = 0
    var flushCalls = 0
    var clearFailure: Exception? = null

    override fun getCookie(url: String): String? = session

    override fun setCookie(url: String, value: String) {
        session = value
    }

    override fun flush() {
        flushCalls++
    }

    override suspend fun clearSession() {
        clearCalls++
        clearFailure?.let { throw it }
        session = null
        flush()
    }
}

class FakeUserRepository(
    private val users: Flow<User?> = MutableStateFlow<User?>(null),
) : UserRepository {

    val savedUsers = mutableListOf<User>()
    var clearCalls = 0

    override fun observeCurrentUser(): Flow<User?> = users

    override suspend fun saveUser(user: User) {
        savedUsers += user
    }

    override suspend fun clearUser() {
        clearCalls++
    }
}
