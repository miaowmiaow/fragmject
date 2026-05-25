package com.example.miaow.base.http

import android.content.Context
import com.example.miaow.base.debug.DebugBridge
import com.example.miaow.base.utils.CacheUtils
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

object OkHelper {

    // OkHttp 磁盘缓存大小：50MB
    private const val CACHE_SIZE_BYTES: Long = 50L * 1024 * 1024

    // 网络超时：连接/读/写均放宽到 15s，对弱网更友好
    private const val TIMEOUT_SECONDS: Long = 15L

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
        val builder = OkHttpClient().newBuilder()
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cookieJar(CookieJar())
            .cache(Cache(CacheUtils.getDirFile(context, "okhttp"), CACHE_SIZE_BYTES))

        // 仅当调用方传入了证书时，才使用自定义 SSL；否则使用系统默认实现，避免传入 null trustManager
        val keyManagers = HttpsHelper.prepareKeyManager(clientCertificate, clientCertificatePwd)
        val trustManager = HttpsHelper.prepareX509TrustManager(serverCertificates)
        if (trustManager != null) {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagers, arrayOf(trustManager), null)
            builder.sslSocketFactory(sslContext.socketFactory, trustManager)
        }

        // 仅 Debug 包打印请求体级日志，Release 包仅打印基本信息，防止账号 / Cookie 等敏感数据泄露
        val loggingLevel = if (DebugBridge.isHttpVerboseLogging) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
        builder.addNetworkInterceptor(HttpLoggingInterceptor().setLevel(loggingLevel))

        return builder.build()
    }
}
