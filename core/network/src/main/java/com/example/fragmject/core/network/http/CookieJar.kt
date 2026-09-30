package com.example.fragmject.core.network.http

import com.example.fragmject.core.android.platform.CookieStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJar : CookieJar {

    // Http 发送请求前回调，Request 中设置 Cookie
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieList: MutableList<Cookie> = ArrayList()
        // CookieStore.getCookie() 期望完整 URL（含 scheme），传 host 在部分系统上可能取不到 Cookie
        CookieStore.getCookie(url.toString())?.let { cookiesStr ->
            if (cookiesStr.isNotEmpty()) {
                val cookies = cookiesStr.split(";".toRegex())
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
            CookieStore.setCookie(url.toString(), cookie.toString())
        }
        CookieStore.flush()
    }

}