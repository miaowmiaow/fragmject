package com.example.miaow.base.http

import android.content.Context
import com.example.miaow.base.debug.DebugBridge
import com.example.miaow.base.utils.CacheUtils
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

object OkHelper {

    // OkHttp 磁盘缓存大小：50MB
    private const val CACHE_SIZE_BYTES: Long = 50L * 1024 * 1024

    // 网络超时：连接/读/写均放宽到 15s，对弱网更友好
    private const val TIMEOUT_SECONDS: Long = 15L

    // 连接池：默认 5 条对首屏并发偏紧，提升到 8 让 banner/top/list/hotkey/tree 同源接口更易复用 TLS 连接
    private const val MAX_IDLE_CONNECTIONS: Int = 8
    private const val KEEP_ALIVE_MINUTES: Long = 5L

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
            // 显式声明 HTTP/2 优先，明确连接复用预期；服务端不支持时 OkHttp 会自动降级到 HTTP/1.1
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_MINUTES, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
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
