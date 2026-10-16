package com.example.fragmject.core.data.repository

import android.util.Log
import com.example.fragmject.core.data.contract.local.UserLocalDataSource
import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.isSuccess
import com.example.fragmject.core.data.contract.remote.UserRemoteDataSource
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.domain.session.CookieStore
import com.example.fragmject.core.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UserRepository] 领域端口的 data 层适配器。
 *
 * 依赖 data-contract 的 [UserRemoteDataSource] 与 [UserLocalDataSource]，
 * 消化网络响应、转换为领域结果，并在 login/register 成功后持久化用户、
 * 显式 logout 无论远端结果如何都尝试清空本地会话。不感知 Retrofit / Room。
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val local: UserLocalDataSource,
    private val cookies: CookieStore,
) : UserRepository {

    private val sessionMutex = Mutex()

    override suspend fun login(username: String, password: String): AuthResult =
        authenticate { remote.login(username, password) }

    override suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): AuthResult = authenticate { remote.register(username, password, repassword) }

    private suspend fun authenticate(request: suspend () -> DataResponse<User>): AuthResult =
        sessionMutex.withLock {
            val previousCookies = try {
                readSessionCookies()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@withLock AuthResult.Error("登录状态读取失败，请重试")
            }
            val response = try {
                request()
            } catch (e: CancellationException) {
                clearChangedSession(previousCookies)?.let { e.addSuppressed(it) }
                throw e
            } catch (_: Exception) {
                val failure = clearChangedSession(previousCookies)
                return@withLock AuthResult.Error(
                    if (failure == null) "认证请求失败，请稍后重试" else "认证请求失败，本地会话清理失败",
                )
            }
            if (!response.isSuccess) {
                val failure = clearChangedSession(previousCookies)
                return@withLock AuthResult.Error(
                    if (failure == null) response.errorMsg else "${response.errorMsg}，本地会话清理失败",
                )
            }
            val user = response.data
            if (user == null) {
                val failure = clearLocalSession()
                return@withLock AuthResult.Error(
                    if (failure == null) "用户信息获取失败" else "用户信息获取失败，本地会话清理失败",
                )
            }
            try {
                local.saveUser(user)
                AuthResult.Success(response.errorMsg)
            } catch (e: CancellationException) {
                clearLocalSession()?.let { e.addSuppressed(it) }
                throw e
            } catch (_: Exception) {
                val failure = clearLocalSession()
                AuthResult.Error(
                    if (failure == null) "登录状态保存失败，请重试" else "登录状态保存失败，本地会话清理失败",
                )
            }
        }

    override suspend fun logout(): Boolean = sessionMutex.withLock {
        var remoteFailure: Exception? = null
        try {
            remote.logout()
            true
        } catch (e: CancellationException) {
            remoteFailure = e
            throw e
        } catch (e: Exception) {
            remoteFailure = e
            true
        } finally {
            clearLocalSession()?.let { cleanupFailure ->
                when (val failure = remoteFailure) {
                    is CancellationException -> failure.addSuppressed(cleanupFailure)
                    null -> {
                        // 远端登出已生效：本地清理失败不能把「成功」翻转成异常，否则 UI 会提示
                        // 登出失败并诱导用户重复点击（远端会话此时已不存在）。残留的本地用户
                        // 数据由下次启动的会话校验兜底，故只记录不抛出。
                        Log.w(TAG, "logout: 远端已登出，但本地会话清理失败", cleanupFailure)
                    }

                    else -> {
                        failure.addSuppressed(cleanupFailure)
                        throw cleanupFailure
                    }
                }
            }
        }
    }

    // 仅比较已知业务根 Cookie；CookieManager 不暴露完整属性，不能安全恢复旧 Cookie。
    private fun readSessionCookies(): List<String?> =
        SESSION_URLS.map { url ->
            cookies.getCookie(url)?.split(';')?.map(String::trim)?.sorted()?.joinToString(";")
        }

    private suspend fun clearChangedSession(previousCookies: List<String?>): Exception? =
        withContext(NonCancellable) {
            val changed = try {
                readSessionCookies() != previousCookies
            } catch (_: Exception) {
                true
            }
            if (changed) clearLocalSession() else null
        }

    private suspend fun clearLocalSession(): Exception? = withContext(NonCancellable) {
        var failure: Exception? = null
        try {
            cookies.clearSession()
        } catch (e: Exception) {
            failure = e
        }
        try {
            local.clearUser()
        } catch (e: Exception) {
            val cookieFailure = failure
            if (cookieFailure == null) failure = e else cookieFailure.addSuppressed(e)
        }
        failure
    }

    override suspend fun saveUser(user: User) {
        sessionMutex.withLock { local.saveUser(user) }
    }

    override fun observeCurrentUser(): Flow<User?> = local.observeCurrentUser()

    private companion object {
        const val TAG = "UserRepositoryImpl"
        val SESSION_URLS = listOf("https://wanandroid.com/", "https://www.wanandroid.com/")
    }
}