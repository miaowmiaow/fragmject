package com.example.fragmject.core.network.http

import android.content.Context
import com.example.fragmject.core.android.platform.CacheUtils
import com.example.fragmject.core.data.contract.http.SslConfig
import com.example.fragmject.core.network.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext

/**
 * OkHttpClient 构建器（无静态状态，可注入、可替换）。
 *
 * 取代原 [OkUtils] 的 object + @Volatile 静态缓存：
 * - 不再持有可变静态状态与「配置变更后失效」逻辑（原 setSslConfig 为死代码）；
 * - CookieJar 由外部注入，便于单测与替换；
 * - SSL 配置作为构建方法的可选参数传入（默认 null = 系统默认校验），不进入 DI 图，
 *   避免 Hilt 试图构造含 Array<InputStream> 的配置对象。
 *
 * 调用方应把构建结果交给 Hilt 以 @Singleton 持有，避免重复创建连接池。
 */
@Singleton
class OkHttpClients @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cookieJar: CookieJar,
) {

    private fun baseBuilder(ssl: SslConfig?): OkHttpClient.Builder {
        val builder = OkHttpClient().newBuilder()
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // 显式声明 HTTP/2 优先，明确连接复用预期；服务端不支持时 OkHttp 会自动降级到 HTTP/1.1
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectionPool(
                ConnectionPool(
                    MAX_IDLE_CONNECTIONS,
                    KEEP_ALIVE_MINUTES,
                    TimeUnit.MINUTES,
                )
            )
            .retryOnConnectionFailure(true)
            .cookieJar(cookieJar)
            .cache(Cache(CacheUtils.getDirFile(context, "okhttp"), CACHE_SIZE_BYTES))

        // 仅当存在有效 SSL 配置时才启用自定义 SSL；否则使用系统默认实现，
        // 避免在未配置证书时传入 null trustManager 或做无意义的 SSLContext 构建。
        if (ssl != null && hasSslConfig(ssl)) {
            val keyManagers = HttpsUtils.prepareKeyManager(ssl.clientCertificate, ssl.clientCertificatePwd)
            val trustManager = HttpsUtils.prepareX509TrustManager(ssl.serverCertificates)
            if (trustManager != null) {
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(keyManagers, arrayOf(trustManager), null)
                builder.sslSocketFactory(sslContext.socketFactory, trustManager)
            }
        }

        // 仅在 Debug 包添加日志拦截器；Release 包完全不添加，避免每个请求上的
        // 日志字符串拼接与拦截器链开销，同时从根上防止账号 / Cookie 等敏感数据经日志泄露。
        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            )
        }
        return builder
    }

    /** 业务请求 client：带 OkHttp 磁盘缓存与 Debug 日志。 */
    fun http(ssl: SslConfig? = null): OkHttpClient = baseBuilder(ssl).build()

    /**
     * 图片加载 / 流式下载专用 client：禁用 OkHttp 磁盘缓存，Debug 下仅保留 HEADERS 日志。
     *
     * 基于 [http] 派生，会共享其连接池与线程池（与历史行为一致）。
     */
    fun noCache(ssl: SslConfig? = null): OkHttpClient = http(ssl).newBuilder()
        .cache(null)
        .apply {
            if (BuildConfig.DEBUG) {
                // 将 BODY 日志降级为 HEADERS，避免大体积响应刷屏。
                networkInterceptors().removeAll { it is HttpLoggingInterceptor }
                addNetworkInterceptor(
                    HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
                )
            }
        }
        .build()

    /** 智能判断：仅当服务端证书非空，或客户端证书 + 密码齐全时才返回 true。 */
    private fun hasSslConfig(ssl: SslConfig): Boolean {
        return (ssl.serverCertificates?.isNotEmpty() == true) ||
            (ssl.clientCertificate != null && !ssl.clientCertificatePwd.isNullOrBlank())
    }

    private companion object {
        /** OkHttp 磁盘缓存大小：50MB */
        const val CACHE_SIZE_BYTES: Long = 50L * 1024 * 1024

        /** 网络超时：连接/读/写均放宽到 15s，对弱网更友好 */
        const val TIMEOUT_SECONDS: Long = 15L

        /** 连接池：默认 5 条对首屏并发偏紧，提升到 8 让同源接口更易复用 TLS 连接 */
        const val MAX_IDLE_CONNECTIONS: Int = 8
        const val KEEP_ALIVE_MINUTES: Long = 5L
    }
}
