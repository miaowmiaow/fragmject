package com.example.fragmject.core.network.http

import android.content.Context
import com.example.fragmject.core.network.BuildConfig
import com.example.fragmject.core.android.platform.CacheUtils
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

object OkUtils {

    // OkHttp 磁盘缓存大小：50MB
    private const val CACHE_SIZE_BYTES: Long = 50L * 1024 * 1024

    // 网络超时：连接/读/写均放宽到 15s，对弱网更友好
    private const val TIMEOUT_SECONDS: Long = 15L

    // 连接池：默认 5 条对首屏并发偏紧，提升到 8 让 banner/top/list/hotkey/tree 同源接口更易复用 TLS 连接
    private const val MAX_IDLE_CONNECTIONS: Int = 8
    private const val KEEP_ALIVE_MINUTES: Long = 5L

    @Volatile
    private var httpClient: OkHttpClient? = null

    // 图片加载（Coil）专用 client：禁用 OkHttp 缓存、Debug 下仅打印 HEADERS 日志。
    @Volatile
    private var imageClient: OkHttpClient? = null

    // 流式下载专用 client：配置同图片 client（禁缓存、Debug 仅 HEADERS 日志），不含离线兜底拦截器。
    @Volatile
    private var downloadClient: OkHttpClient? = null

    // SSL 相关配置：默认均为 null（不启用自定义 SSL），后续接入证书时通过 setSslConfig 注入。
    private var clientCertificate: InputStream? = null
    private var clientCertificatePwd: String? = null
    private var serverCertificates: Array<InputStream>? = null

    /**
     * 配置自定义 SSL 证书（供后续快速接入双向/单向 TLS）。
     *
     * - [clientCertificate] 客户端 PKCS12 证书流（双向 TLS 时使用），可为 null；
     * - [clientCertificatePwd] 客户端证书密码；
     * - [serverCertificates] 服务端证书流数组（用于替换默认 CA 校验），可为 null。
     *
     * 至少配置其中之一才会启用自定义 SSL，否则回落到系统默认实现。
     * 调用后会使已缓存的 [httpClient] 失效，下次获取时按新配置重建。
     *
     * ===== 自定义 SSL 证书接入示例（默认关闭，需要时取消注释） =====
     * 证书文件放在 assets 或 res/raw 下，按需读取后注入即可；
     * 至少配置服务端证书（serverCertificates）或客户端证书（clientCertificate+密码）之一才会启用，
     * 未配置时自动回落到系统默认 SSL 校验，无性能损耗。
     *
     * 场景一：单向 TLS（固定服务端证书，替换默认 CA 校验）
     * OkUtils.setSslConfig(
     *     serverCertificates = arrayOf(assets.open("server.crt")),
     * )
     *
     * 场景二：双向 TLS（同时校验客户端证书）
     * OkUtils.setSslConfig(
     *     clientCertificate = assets.open("client.p12"),
     *     clientCertificatePwd = "your_password",
     *     serverCertificates = arrayOf(assets.open("server.crt")),
     * )
     * ===== 示例结束 =====
     */
    @Synchronized
    fun setSslConfig(
        clientCertificate: InputStream? = null,
        clientCertificatePwd: String? = null,
        serverCertificates: Array<InputStream>? = null,
    ) {
        this.clientCertificate = clientCertificate
        this.clientCertificatePwd = clientCertificatePwd
        this.serverCertificates = serverCertificates
        // 配置变更后需重建 client，避免沿用旧连接配置
        httpClient = null
    }

    /**
     * 智能判断：是否存在有效的 SSL 配置。
     * 仅当服务端证书非空，或客户端证书 + 密码齐全时才返回 true。
     */
    private fun hasSslConfig(): Boolean {
        return (serverCertificates?.isNotEmpty() == true) ||
                (clientCertificate != null && !clientCertificatePwd.isNullOrBlank())
    }

    @JvmStatic
    fun httpClient(context: Context): OkHttpClient = httpClient ?: synchronized(this) {
        // 双重检查锁：仅在尚未初始化时进入同步块；初始化后无锁竞争直接返回。
        httpClient ?: getOkHttpBuilder(context).also { httpClient = it }
    }

    /**
     * 图片加载（Coil）专用 OkHttpClient。
     *
     * 与业务请求 client 分离：
     * - 禁用 OkHttp 磁盘缓存，避免与 Coil 自带磁盘缓存形成双重缓存；
     * - Debug 包仅保留 HEADERS 日志（图片响应体量大，BODY 日志刷屏且无意义），
     *   Release 包依旧不加任何日志拦截器。
     */
    @JvmStatic
    fun imageClient(context: Context): OkHttpClient = imageClient ?: synchronized(this) {
        imageClient ?: buildNoCacheClient(context).also { imageClient = it }
    }

    /**
     * 流式下载专用 OkHttpClient。
     *
     * 基于裸 client（不含离线兜底拦截器）正向构建：
     * - 禁用 OkHttp 磁盘缓存，避免大文件写缓存导致 SocketException；
     * - Debug 包仅保留 HEADERS 日志，Release 包不加任何日志拦截器。
     */
    @JvmStatic
    fun downloadClient(context: Context): OkHttpClient = downloadClient ?: synchronized(this) {
        downloadClient ?: buildNoCacheClient(context).also { downloadClient = it }
    }

    /**
     * 构建「禁用 OkHttp 磁盘缓存、Debug 下仅保留 HEADERS 日志」的 client。
     * 图片加载与流式下载共用：两者都不需要 OkHttp 磁盘缓存，也都无需打印 BODY 日志。
     */
    private fun buildNoCacheClient(context: Context): OkHttpClient =
        httpClient(context).newBuilder()
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

    private fun getOkHttpBuilder(context: Context): OkHttpClient {
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
                    TimeUnit.MINUTES
                )
            )
            .retryOnConnectionFailure(true)
            .cookieJar(CookieJar())
            .cache(Cache(CacheUtils.getDirFile(context, "okhttp"), CACHE_SIZE_BYTES))

        // 智能判断：仅当存在有效 SSL 配置时才启用自定义 SSL；否则使用系统默认实现。
        // 避免在未配置证书时传入 null trustManager 或做无意义的 SSLContext 构建。
        if (hasSslConfig()) {
            val keyManagers = HttpsUtils.prepareKeyManager(clientCertificate, clientCertificatePwd)
            val trustManager = HttpsUtils.prepareX509TrustManager(serverCertificates)
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

        return builder.build()
    }
}
