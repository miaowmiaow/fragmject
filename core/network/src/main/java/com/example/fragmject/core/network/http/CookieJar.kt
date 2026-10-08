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
        val cookieList: MutableList<Cookie> = ArrayList()
        // CookieStore.getCookie() 期望完整 URL（含 scheme），传 host 在部分系统上可能取不到 Cookie
        store.getCookie(url.toString())?.let { cookiesStr ->
            if (cookiesStr.isNotEmpty()) {
                // 用字符重载而非 ";" .toRegex()：toRegex() 每次都会 Pattern.compile，
                // 而 loadForRequest 是 OkHttp 每请求必调的回调
                val cookies = cookiesStr.split(';')
                for (cookie in cookies) {
                    Cookie.parse(url, cookie.trim())?.apply {
                        cookieList.add(this)
                    }
                }
            }
        }
        return cookieList
    }

    // Http 请求结束，Response 中有 Cookie 时候回调
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            store.setCookie(url.toString(), cookie.toString())
        }
        store.flush()
    }
}
