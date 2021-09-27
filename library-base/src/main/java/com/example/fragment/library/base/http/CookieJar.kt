package com.example.fragment.library.base.http

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJar : CookieJar {

    //Http发送请求前回调，Request中设置Cookie
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieList: MutableList<Cookie> = ArrayList()
        CookieManager.getInstance().getCookie(url.toString())?.let { cookiesStr ->
            if (cookiesStr.isNotEmpty()) {
                val cookies = cookiesStr.split(";".toRegex())
                for (cookie in cookies) {
                    Cookie.parse(url, cookie)?.apply {
                        cookieList.add(this)
                    }
                }
            }
        }
        return cookieList
    }

    //Http请求结束，Response中有Cookie时候回调
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        for (cookie in cookies) {
            cookieManager.setCookie(url.toString(), cookie.toString())
        }
        cookieManager.flush()
    }

}