package com.example.fragmject.core.webview

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import com.example.fragmject.core.domain.session.CookieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CookieStore] 的 WebView 实现。
 *
 * [CookieManager.getInstance()] 是进程内单例，因此 OkHttp 与 WebView 必然共享同一份会话，
 * 这也是本实现放在 core:webview 的依据。
 *
 * 同步写入只允许在后台线程调用：提交到主线程并等待 CookieManager 回调确认，
 * 避免尚未完成 setCookie 就 flush。清理使用后台线程，按业务域/已知 Path 定向过期，
 * 不使用 removeAllCookies，避免影响用户浏览其他站点的会话。
 * CookieManager 不提供完整的 Domain/Path 枚举；历史未观察路径及其他业务子域不保证覆盖，
 * 也不负责撤销服务端令牌或阻止清理之后的迟到响应重新写入 Cookie。
 * 超时/中断会撤销未执行的排队任务，已提交 CookieManager 的写入不能撤回。
 *
 * 相关但不属于本端口的能力：`setAcceptThirdPartyCookies` 需要 android.webkit.WebView 实例，
 * 由 [WebViewPoolManager] 在创建 WebView 时直接设置，避免 WebView 类型进入 domain 契约。
 */
@Singleton
class WebViewCookieStore @Inject constructor() : CookieStore {

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance().apply { setAcceptCookie(true) }
    }

    private val writeLock = Any()
    private val sessionPaths = ConcurrentSkipListSet<String>().apply { add("/") }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun getCookie(url: String): String? = cookieManager.getCookie(url)

    override fun setCookie(url: String, value: String) {
        requireBackgroundThread()
        synchronized(writeLock) {
            rememberPaths(url, value)
            writeCookie(url, value)
        }
    }

    override fun flush() {
        requireBackgroundThread()
        synchronized(writeLock) { cookieManager.flush() }
    }

    override suspend fun clearSession(): Unit = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            val paths = sessionPaths.toList()
            val names = linkedSetOf<String>()
            for (host in SESSION_HOSTS) {
                for (path in paths) {
                    cookieManager.getCookie("https://$host$path")
                        ?.split(';')
                        ?.mapNotNullTo(names) { it.substringBefore('=').trim().takeIf(String::isNotEmpty) }
                }
            }
            var failure: Exception? = null
            for (host in SESSION_HOSTS) {
                for (path in paths) {
                    for (name in names) {
                        val expired = "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=$path; Secure; HttpOnly"
                        val domains = listOf(null, "wanandroid.com") +
                            if (host == "www.wanandroid.com") listOf(host) else emptyList()
                        for (domain in domains) {
                            try {
                                writeCookie("https://$host$path", expired + (domain?.let { "; Domain=$it" } ?: ""))
                            } catch (e: Exception) {
                                val previous = failure
                                if (previous == null) failure = e else previous.addSuppressed(e)
                            }
                        }
                    }
                }
            }
            try {
                cookieManager.flush()
            } catch (e: Exception) {
                val previous = failure
                if (previous == null) failure = e else previous.addSuppressed(e)
            }
            failure?.let { throw it }
            for (host in SESSION_HOSTS) {
                for (path in paths) {
                    if (!cookieManager.getCookie("https://$host$path").isNullOrBlank()) {
                        throw IOException("业务会话 Cookie 清理未完成")
                    }
                }
            }
        }
    }

    private fun rememberPaths(url: String, value: String) {
        val uri = runCatching { URI(url) }.getOrNull() ?: return
        if (uri.host !in SESSION_HOSTS) return
        val explicitPath = value.split(';')
            .firstOrNull { it.trim().startsWith("path=", ignoreCase = true) }
            ?.substringAfter('=')?.trim()?.takeIf { it.startsWith('/') }
        val requestPath = uri.rawPath.orEmpty()
        val defaultPath = requestPath.substringBeforeLast('/', "").ifEmpty { "/" }
        val path = explicitPath ?: defaultPath
        if (';' !in path) sessionPaths.add(path)
    }

    private fun writeCookie(url: String, value: String) {
        val done = CountDownLatch(1)
        var accepted = false
        var failure: Exception? = null
        val abandoned = AtomicBoolean(false)
        val write = Runnable {
            if (!abandoned.get()) {
                try {
                    cookieManager.setCookie(url, value) {
                        accepted = it
                        done.countDown()
                    }
                } catch (e: Exception) {
                    failure = e
                    done.countDown()
                }
            }
        }
        if (!mainHandler.post(write)) throw IOException("Cookie 写入提交失败")
        try {
            if (!done.await(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                abandoned.set(true)
                mainHandler.removeCallbacks(write)
                throw IOException("Cookie 写入确认超时")
            }
        } catch (e: InterruptedException) {
            abandoned.set(true)
            mainHandler.removeCallbacks(write)
            Thread.currentThread().interrupt()
            throw IOException("Cookie 写入等待被中断", e)
        }
        failure?.let { throw it }
        if (!accepted) throw IOException("Cookie 写入被拒绝")
    }

    private fun requireBackgroundThread() {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "同步 Cookie 写入与持久化必须在后台线程调用"
        }
    }

    private companion object {
        val SESSION_HOSTS = arrayOf("wanandroid.com", "www.wanandroid.com")
        const val WRITE_TIMEOUT_SECONDS = 10L
    }
}
