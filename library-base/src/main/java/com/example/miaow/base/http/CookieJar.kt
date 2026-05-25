package com.example.miaow.base.http

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJar : CookieJar {

    private val cookieManager: CookieManager = CookieManager.getInstance().apply {
        setAcceptCookie(true)
    }

    //Http发送请求前回调，Request中设置Cookie
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieList: MutableList<Cookie> = ArrayList()
        // CookieManager.getCookie() 期望完整 URL（含 scheme），传 host 在部分系统上可能取不到 Cookie
        cookieManager.getCookie(url.toString())?.let { cookiesStr ->
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

    //Http请求结束，Response中有Cookie时候回调
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            cookieManager.setCookie(url.toString(), cookie.toString())
        }
        cookieManager.flush()
    }

}