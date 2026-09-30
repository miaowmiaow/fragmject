package com.example.fragmject.core.webview

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

/**
 * WebView 资源缓存与加载优化契约。
 *
 * 与 [WebViewPool]（池生命周期）解耦，单独定义在 core:webview：
 * - HTML 主文档缓存（stale-while-revalidate）；
 * - 静态资源（图片/样式/脚本/字体）磁盘缓存；
 * - DNS 预解析、缓存溢出淘汰等加载优化。
 *
 * 实现见 [WebResourceCacheManager]（由 Hilt 以单例提供）。
 */
interface WebResourceCache {

    /**
     * 判断请求是否为可缓存的 HTML 主文档（主框架 + GET + Accept 含 text/html）。
     */
    fun isCacheableHtml(request: WebResourceRequest): Boolean

    /**
     * HTML 主文档缓存（stale-while-revalidate 语义）：
     * - 命中未硬过期的缓存则直接返回响应，并后台异步刷新；
     * - 未命中/硬过期返回 null，并后台异步下载填充。
     */
    fun cacheHtmlRequest(context: Context, request: WebResourceRequest): WebResourceResponse?

    /** 判断请求是否为可缓存资源（图片/样式/脚本/字体等）。 */
    fun isCacheableResource(request: WebResourceRequest): Boolean

    /**
     * 命中本地磁盘缓存则返回响应；未命中返回 null 并异步下载填充缓存。
     *
     * [referer] 为发起该资源请求的页面 URL（即 WebView 当前加载的页面），
     * 用于给异步下载补上防盗链所需的 Referer 请求头。
     */
    fun cacheResourceRequest(
        context: Context,
        request: WebResourceRequest,
        referer: String?,
    ): WebResourceResponse?

    /** 预解析 URL 的主机名，降低首次连接延迟。 */
    fun prefetchDns(url: String)

    /** 触发缓存维护：文件数超限时按 mtime 淘汰最旧文件，内部异步执行，冷启动零开销。 */
    fun evictByMtimeIfNeeded(context: Context)
}
