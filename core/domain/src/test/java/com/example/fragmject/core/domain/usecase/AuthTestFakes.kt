package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.auth.AuthGateway
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.model.User
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 认证相关用例的共享测试替身：全部是纯 JVM 实现，不碰 Android。 */
internal class FakeCookieStore : CookieStore {
    var session: String? = "session=active"
    var clearCalls = 0
    var flushCalls = 0
    var clearFailure: Exception? = null
    var readFailure: Exception? = null

    override fun getCookie(url: String): String? {
        readFailure?.let { throw it }
        return session
    }

    override fun setCookie(url: String, value: String) {
        session = value
    }

    override fun flush() {
        flushCalls++
    }

    override suspend fun clearSession() {
        currentCoroutineContext().ensureActive()
        clearCalls++
        clearFailure?.let { throw it }
        session = null
        flush()
    }
}

internal class FakeUserRepository : UserRepository {
    private val userFlow = MutableStateFlow<User?>(null)
    var saveFailure: Exception? = null
    var clearFailure: Exception? = null
    var clearCalls = 0

    fun current(): User? = userFlow.value

    fun seed(user: User?) {
        userFlow.value = user
    }

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

internal class FakeAuthGateway(
    private val loginHandler: suspend () -> AuthExchange = {
        AuthExchange.Success(User(id = 1, username = "alice"), "登录成功")
    },
    private val registerHandler: suspend () -> AuthExchange = {
        AuthExchange.Success(User(id = 2, username = "bob"), "注册成功")
    },
) : AuthGateway {
    var loginCalls = 0
    var registerCalls = 0
    var lastArguments: List<String> = emptyList()
    var logoutBehavior: suspend () -> Unit = {}

    override suspend fun login(username: String, password: String): AuthExchange {
        loginCalls++
        lastArguments = listOf(username, password)
        return loginHandler()
    }

    override suspend fun register(username: String, password: String, repassword: String): AuthExchange {
        registerCalls++
        lastArguments = listOf(username, password, repassword)
        return registerHandler()
    }

    override suspend fun logout() {
        logoutBehavior()
    }
}
