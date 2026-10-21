package com.example.fragmject.core.network.http

import com.example.fragmject.core.domain.session.CookieStore
import okhttp3.Cookie
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp 与 WebView 的 Cookie 桥接。
 *
 * 经 core:domain 的 [CookieStore] 端口读写，使 core:network 无需（也不允许）
 * 反向依赖 core:webview，同时便于用 fake 实现做单测。
 */
@Singleton
class CookieJar @Inject constructor(
    private val store: CookieStore,
) : okhttp3.CookieJar {

    // Http 发送请求前回调，Request 中设置 Cookie
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // CookieStore.getCookie() 期望完整 URL（含 scheme），传 host 在部分系统上可能取不到 Cookie
        val cookiesStr = store.getCookie(url.toString()).orEmpty()
        if (cookiesStr.isEmpty()) return emptyList()
        // 用字符重载而非 ";" .toRegex()：toRegex() 每次都会 Pattern.compile，
        // 而 loadForRequest 是 OkHttp 每请求必调的回调
        return cookiesStr.split(';').mapNotNull { Cookie.parse(url, it.trim()) }
    }

    // Http 请求结束，Response 中有 Cookie 时候回调
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        var writeFailure: Exception? = null
        try {
            for (cookie in cookies) {
                store.setCookie(url.toString(), cookie.toString())
            }
        } catch (e: Exception) {
            writeFailure = e
        }
        try {
            store.flush()
        } catch (e: Exception) {
            // flush 失败并入首个失败：既保留写入异常，也不丢持久化异常
            if (writeFailure == null) writeFailure = e else writeFailure.addSuppressed(e)
        }
        writeFailure?.let { throw it }
    }
}
