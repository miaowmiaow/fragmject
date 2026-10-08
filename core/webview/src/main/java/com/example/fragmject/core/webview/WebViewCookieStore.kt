package com.example.fragmject.core.webview

import android.webkit.CookieManager
import com.example.fragmject.core.domain.session.CookieStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CookieStore] 的 WebView 实现。
 *
 * [CookieManager.getInstance()] 是进程内单例，因此 OkHttp 与 WebView 必然共享同一份会话，
 * 这也是本实现放在 core:webview 的依据。
 *
 * 端口为同步形态：CookieManager 自身线程安全且开销极小，这里不再切线程，
 * 由调用方（OkHttp 的同步回调）直接调用，避免异步化导致的读写时序不确定。
 *
 * 相关但不属于本端口的能力：`setAcceptThirdPartyCookies` 需要 android.webkit.WebView 实例，
 * 由 [WebViewPoolManager] 在创建 WebView 时直接设置，避免 WebView 类型进入 domain 契约。
 */
@Singleton
class WebViewCookieStore @Inject constructor() : CookieStore {

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance().apply { setAcceptCookie(true) }
    }

    override fun getCookie(url: String): String? = cookieManager.getCookie(url)

    override fun setCookie(url: String, value: String) {
        cookieManager.setCookie(url, value)
    }

    override fun flush() {
        cookieManager.flush()
    }
}
