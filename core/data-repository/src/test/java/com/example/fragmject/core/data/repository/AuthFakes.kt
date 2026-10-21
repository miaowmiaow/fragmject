package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.User
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 认证相关 data 层测试的共享替身（供 [AuthGatewayImplTest] 与 [UserRepositoryImplTest] 使用）。 */
internal class FakeUserLocalDataSource : UserLocalDataSource {
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

internal class FakeUserRemoteDataSource(
    private val loginResult: suspend (String, String) -> DataResponse<User> = { _, _ -> DataResponse() },
    private val registerResult: suspend (String, String, String) -> DataResponse<User> =
        { _, _, _ -> DataResponse() },
    private val logoutResult: suspend () -> HttpResponse = { HttpResponse() },
) : UserRemoteDataSource {
    var logoutCalls = 0

    override suspend fun login(username: String, password: String): DataResponse<User> =
        loginResult(username, password)

    override suspend fun register(username: String, password: String, repassword: String): DataResponse<User> =
        registerResult(username, password, repassword)

    override suspend fun logout(): HttpResponse {
        logoutCalls++
        return logoutResult()
    }

    override suspend fun getUserShareArticles(userId: String, page: Int): DataResponse<ShareArticle> =
        DataResponse()
}

internal class FakeSessionCookieStore : CookieStore {
    var session: String? = "session=active"
    var clearCalls = 0
    var flushCalls = 0

    override fun getCookie(url: String): String? = session

    override fun setCookie(url: String, value: String) {
        session = value
    }

    override fun flush() {
        flushCalls++
    }

    override suspend fun clearSession() {
        currentCoroutineContext().ensureActive()
        clearCalls++
        session = null
        flush()
    }
}
