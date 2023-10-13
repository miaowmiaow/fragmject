package com.example.miaow.base.http

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

object OkHelper {

    private var httpClient: OkHttpClient? = null

    private var clientCertificate: InputStream? = null
    private var clientCertificatePwd: String? = null
    private var serverCertificates: Array<InputStream>? = null

    @JvmStatic
    @Synchronized
    fun httpClient(context: Context): OkHttpClient = httpClient ?: getOkHttpBuilder(context).also {
        httpClient = it
    }

    private fun getOkHttpBuilder(context: Context): OkHttpClient {
        val keyManagers = HttpsHelper.prepareKeyManager(clientCertificate, clientCertificatePwd)
        val trustManager = HttpsHelper.prepareX509TrustManager(serverCertificates)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, arrayOf(trustManager), null)
        return OkHttpClient().newBuilder()
            .readTimeout(10000L, TimeUnit.MILLISECONDS)
            .writeTimeout(10000L, TimeUnit.MILLISECONDS)
            .connectTimeout(10000L, TimeUnit.MILLISECONDS)
            .cookieJar(CookieJar())
            .cache(Cache(File(context.cacheDir, "okhttp"), 250L * 1024 * 1024))
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
    }
}
