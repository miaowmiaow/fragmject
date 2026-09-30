package com.example.fragmject.core.android.platform

import android.webkit.CookieManager
import android.webkit.WebView

/**
 * WebView Cookie 管理器封装。
 *
 * 将 [CookieManager] 的直接依赖收敛到 android-platform，
 * 供 core:network（CookieJar）与 core:webview（缓存/池管理）复用，
 * 避免各模块各自直接触碰 android.webkit.CookieManager。
 */
object CookieStore {

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance().apply { setAcceptCookie(true) }
    }

    /** 读取指定 URL 的 Cookie（期望完整 URL 含 scheme）。 */
    fun getCookie(url: String): String? = cookieManager.getCookie(url)

    /** 写入 Cookie。 */
    fun setCookie(url: String, value: String) {
        cookieManager.setCookie(url, value)
    }

    /** 将内存中的 Cookie 立即持久化。 */
    fun flush() {
        cookieManager.flush()
    }

    /** 设置是否接受第三方 Cookie（WebView 场景）。 */
    fun setAcceptThirdPartyCookies(webView: WebView, accept: Boolean) {
        cookieManager.setAcceptThirdPartyCookies(webView, accept)
    }
}
