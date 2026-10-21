package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.domain.result.AuthExchange
import com.example.fragmject.core.domain.result.AuthResult
import com.example.fragmject.core.domain.session.CookieStore
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 认证会话编排（应用层）：把「一次认证请求」与「会话一致性」绑在一起。
 *
 * 为什么在 domain 而不是 Repository：
 * - 这里是不变量而不是存储细节 —— 「认证失败不能留下半登录态」「写库失败要连带清 Cookie」
 *   「取消要原样传播但仍完成清理」，这些规则不应埋在 data 层实现里；
 * - 放在 domain 后，[UserRepository] 只剩 observe/save/clear 三个原子操作，
 *   [AuthGateway] 只做 transport，编排可以被纯 JVM 单测直接覆盖（[AuthSessionCoordinatorTest]）。
 *
 * 并发模型：登录/注册/登出共用一把 [Mutex]，避免两次认证交错写 Cookie 与用户表。
 */
class AuthSessionCoordinator @Inject constructor(
    private val cookies: CookieStore,
    private val users: UserRepository,
) {

    private val mutex = Mutex()

    /**
     * 执行一次认证并保证会话一致：
     * 请求前快照 Cookie → 请求 → 失败时按需回滚 → 成功时持久化用户（失败则清理）。
     */
    suspend fun authenticate(exchange: suspend () -> AuthExchange): AuthResult = mutex.withLock {
        val previousCookies = try {
            readSessionCookies()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withLock AuthResult.Error("登录状态读取失败，请重试")
        }
        val result = try {
            exchange()
        } catch (e: CancellationException) {
            clearChangedSession(previousCookies)?.let { e.addSuppressed(it) }
            throw e
        } catch (_: Exception) {
            return@withLock AuthResult.Error(
                withCleanupFailure("认证请求失败，请稍后重试", clearChangedSession(previousCookies)),
            )
        }
        when (result) {
            is AuthExchange.Success -> persist(result)
            is AuthExchange.Failure -> AuthResult.Error(
                withCleanupFailure(
                    result.message,
                    // HTTP 成功但缺用户体：会话可能已建立，必须无条件清理；
                    // 普通失败只在 Cookie 真的变了时才回滚，避免误清既有登录态。
                    if (result.sessionTouched) clearLocalSession() else clearChangedSession(previousCookies),
                ),
            )
        }
    }

    /** 登出编排：远端 best effort，本地必须清理；异常优先级见方法内注释。 */
    suspend fun logout(remoteLogout: suspend () -> Unit): Boolean = mutex.withLock {
        var remoteFailure: Exception? = null
        try {
            remoteLogout()
        } catch (e: CancellationException) {
            remoteFailure = e
        } catch (e: Exception) {
            remoteFailure = e
        }

        // 本地清理不在 finally 里抛异常：异常只在这里统一抛出，
        // 优先级保持 —— 取消信号 > 远端异常 > 成功。
        val cleanupFailure = clearLocalSession()
        (remoteFailure as? CancellationException)?.let { cancellation ->
            cleanupFailure?.let { cancellation.addSuppressed(it) }
            throw cancellation
        }
        if (cleanupFailure != null && remoteFailure != null) {
            remoteFailure.addSuppressed(cleanupFailure)
            throw cleanupFailure
        }
        // 远端登出已生效：本地清理失败不能把「成功」翻转成异常，否则 UI 会提示
        // 登出失败并诱导用户重复点击（远端会话此时已不存在）。残留数据由下次启动的
        // 会话校验兜底。domain 不依赖 android.util.Log，故这里只吞掉不记录。
        true
    }

    /** 认证成功后持久化用户；写库失败与取消都要清掉本地会话，避免半登录状态。 */
    private suspend fun persist(exchange: AuthExchange.Success): AuthResult {
        return try {
            users.saveUser(exchange.user)
            AuthResult.Success(exchange.message)
        } catch (e: CancellationException) {
            clearLocalSession()?.let { e.addSuppressed(it) }
            throw e
        } catch (_: Exception) {
            AuthResult.Error(withCleanupFailure("登录状态保存失败，请重试", clearLocalSession()))
        }
    }

    /** 清理失败不改变主因，只在用户可见文案后追加提示。 */
    private fun withCleanupFailure(message: String, failure: Exception?): String =
        if (failure == null) message else "$message，本地会话清理失败"

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
            users.clearUser()
        } catch (e: Exception) {
            val cookieFailure = failure
            if (cookieFailure == null) failure = e else cookieFailure.addSuppressed(e)
        }
        failure
    }

    private companion object {
        val SESSION_URLS = listOf("https://wanandroid.com/", "https://www.wanandroid.com/")
    }
}
