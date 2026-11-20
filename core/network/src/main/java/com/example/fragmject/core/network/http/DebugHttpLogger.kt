package com.example.fragmject.core.network.http

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Debug 包的 HTTP 日志（替代 `HttpLoggingInterceptor.Level.BODY`）。
 *
 * ## 为什么不用 `HttpLoggingInterceptor`
 *
 * 它的 `BODY` 级会把**请求体原文**打进 logcat，而登录 / 注册的凭据就在表单体里
 * （`username=...&password=...`），等于把明文密码写进系统日志缓冲区。`Level.BODY` 只区分
 * 「有没有日志」，不区分「这一条能不能打」。
 *
 * ## 本实现的边界
 *
 * - **请求体一律不打印**：凭据、token 都在这里，且这条规则不依赖「认识哪些端点」，
 *   将来新增敏感接口也不会漏（这是与「按路径白名单」最重要的区别）；
 * - **凭据提交端点**（[isCredentialSubmission]）额外屏蔽 URL query 与响应体：
 *   登录响应体含用户资料与会话标识，同样不该进日志；
 * - **响应体**只在非凭据端点、且是文本类响应时截断打印（≤ [MAX_BODY_PREVIEW_BYTES]），
 *   保住了实际调试最常用的那部分信息；
 * - **敏感头一律打 `<redacted>`**：`Cookie` / `Set-Cookie` / `Authorization` / `Proxy-Authorization`。
 *
 * ## 挂在应用拦截器上的原因
 *
 * 应用拦截器位于 `BridgeInterceptor` 之外：请求侧还没有被 CookieJar 塞入 `Cookie`，
 * 响应侧已经完成 gzip 解压（网络拦截器看到的还是压缩流，`peekBody().string()` 会是乱码）。
 *
 * 仅在 Debug 包安装（见 [OkHttpClients.baseBuilder]），Release 包完全不添加，
 * 从根上避免日志拼接开销与任何日志泄露面。
 */
internal class DebugHttpLogger(
    /** 流式下载 / 图片等大响应场景关掉，只保留请求行与头部。 */
    private val logResponseBody: Boolean = true,
    /** 便于单测注入；默认走 logcat。 */
    private val sink: (String) -> Unit = { Log.d(TAG, it) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sensitive = isCredentialSubmission(request.method, request.url)
        val startNs = System.nanoTime()

        sink("--> ${request.method} ${displayUrl(request.url, sensitive)}")
        request.headers.forEach { (name, value) -> sink("$name: ${maskedValue(name, value)}") }
        if (request.body != null) sink("--> (request body omitted)")

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            sink("<-- ${request.method} ${displayUrl(request.url, sensitive)} FAILED (${e.javaClass.simpleName})")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        sink("<-- ${response.code} ${request.method} ${displayUrl(request.url, sensitive)} (${tookMs}ms)")
        response.headers.forEach { (name, value) -> sink("$name: ${maskedValue(name, value)}") }
        if (logResponseBody && !sensitive) {
            bodyPreview(response)?.let { sink("<-- body: $it") }
        }
        return response
    }

    /** 文本类响应才取预览；二进制（图片 / 音视频）直接跳过，避免刷屏与无意义的内存拷贝。 */
    private fun bodyPreview(response: Response): String? {
        val mediaType = response.body.contentType() ?: return null
        val type = mediaType.type.lowercase()
        val subtype = mediaType.subtype.lowercase()
        val textual = type == "text" ||
            subtype.contains("json") ||
            subtype.contains("xml") ||
            subtype.contains("javascript")
        if (!textual) return null
        // peekBody 在 Response 上（ResponseBody 没有该方法），返回的副本消费掉不影响原响应
        return runCatching { response.peekBody(MAX_BODY_PREVIEW_BYTES).string() }.getOrNull()
    }

    private fun displayUrl(url: HttpUrl, sensitive: Boolean): String =
        if (sensitive && url.query != null) url.newBuilder().query(null).build().toString() else url.toString()

    private fun maskedValue(name: String, value: String): String =
        if (name.lowercase() in SENSITIVE_HEADERS) REDACTED else value

    private companion object {
        const val TAG = "Http"
        const val REDACTED = "<redacted>"
        const val MAX_BODY_PREVIEW_BYTES = 4L * 1024

        /**
         * 头部黑名单按小写比对（OkHttp 的 header 名大小写不敏感，值不保证统一）。
         */
        val SENSITIVE_HEADERS = setOf(
            "cookie",
            "set-cookie",
            "authorization",
            "proxy-authorization",
        )
    }
}

/**
 * 是否属于「提交凭据」的端点：只认 POST，路径按段前缀匹配。
 *
 * 目前是玩 Android 的 `user/login` 与 `user/register`；`user/logout/json` 虽然是 GET、
 * 但用户明确要求屏蔽范围只覆盖凭据提交，故不在其列（其响应体不含凭据，
 * `Set-Cookie` 由头部黑名单处理）。
 */
internal fun isCredentialSubmission(method: String, url: HttpUrl): Boolean {
    if (!method.equals("POST", ignoreCase = true)) return false
    val path = url.encodedPath.lowercase()
    return CREDENTIAL_PATHS.any { path == it || path.startsWith("$it/") }
}

private val CREDENTIAL_PATHS = listOf("/user/login", "/user/register")
