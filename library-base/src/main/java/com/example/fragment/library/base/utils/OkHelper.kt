package com.example.fragment.library.base.utils

import android.content.Context
import com.example.fragment.library.base.http.CookieJar
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

object OkHelper {

    private var httpClient: OkHttpClient? = null

    @JvmStatic
    @Synchronized
    fun httpClient(context: Context): OkHttpClient = httpClient ?: OkHttpClient().newBuilder()
        .readTimeout(15000L, TimeUnit.MILLISECONDS)
        .writeTimeout(15000L, TimeUnit.MILLISECONDS)
        .connectTimeout(15000L, TimeUnit.MILLISECONDS)
        .cookieJar(CookieJar())
        .cache(Cache(File(context.cacheDir, "okhttp"), 250L * 1024 * 1024))
        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build().also {
            httpClient = it
        }
}
