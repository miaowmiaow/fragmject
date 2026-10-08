package com.example.fragmject.core.webview

import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.example.fragmject.core.android.platform.AppScope
import com.example.fragmject.core.android.platform.CacheUtils
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.session.CookieStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.example.fragmject.core.android.platform.DigestUtils
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * WebView 资源缓存实现：本地静态资源磁盘缓存 + HTML 主文档缓存 + 加载优化。
 *
 * 从 [WebViewPoolManager] 拆出，使池管理与缓存职责各自独立：
 * - 静态资源（图片/样式/脚本/字体）按文件 mtime 淘汰，冷启动零开销；
 * - HTML 主文档采用 stale-while-revalidate：命中返回缓存并后台刷新，保证首屏提速的同时内容不永久陈旧。
 */
@Singleton
class WebResourceCacheManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val downloader: DownloadRepository,
    private val cookieStore: CookieStore,
) : WebResourceCache {

    companion object {
        private const val TAG = "WebResourceCache"
        private const val DOWNLOAD_TIMEOUT_MS = 8_000L
        private const val WEB_CACHE_DIR = "web_cache"

        /** 本地缓存文件数上限。超过时按 mtime 淘汰最旧文件，mtime 在每次命中时被 touch。 */
        private const val WEB_CACHE_MAX_FILES = 5000

        private const val ACCEPT_IMAGE =
            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

        private val CACHEABLE_EXTENSIONS = setOf(
            "ico", "bmp", "gif", "jpeg", "jpg", "png", "svg", "webp",
            "css", "js", "json",
            "eot", "otf", "ttf", "woff"
        )

        /** HTML 主文档独立缓存目录，与静态资源缓存解耦（独立 TTL/上限）。 */
        private const val HTML_CACHE_DIR = "web_html_cache"

        /** HTML 缓存文件数上限，超过时按 mtime 淘汰最旧文件。 */
        private const val HTML_CACHE_MAX_FILES = 200

        /** HTML 硬过期时长：超过则强制回源，避免后台刷新持续失败导致内容永久陈旧。 */
        private const val HTML_HARD_EXPIRE_MS = 7L * 24 * 60 * 60 * 1000
    }

    /** 正在下载中的缓存任务，按缓存 key 去重，避免并发重复下载同一资源。 */
    private val inFlightDownloads: ConcurrentHashMap<String, Job> = ConcurrentHashMap()

    /**
     * 缓存目录（解析一次）。
     *
     * 每次拦截都调 CacheUtils.getDirPath 会执行 mkdirs 并产生多轮磁盘 stat，
     * 叠加在 shouldInterceptRequest 线程上拖慢首屏，故在此缓存。
     */
    private val webCachePath: String by lazy { CacheUtils.getDirPath(appContext, WEB_CACHE_DIR) }
    private val htmlCachePath: String by lazy { CacheUtils.getDirPath(appContext, HTML_CACHE_DIR) }

    override fun isCacheableHtml(request: WebResourceRequest): Boolean {
        if (!request.method.equals("GET", true)) return false
        if (!request.isForMainFrame) return false
        val url = request.url.toString()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val accept = request.requestHeaders.entries
            .firstOrNull { it.key.equals("Accept", true) }?.value ?: return false
        return accept.contains("text/html", ignoreCase = true)
    }

    override fun cacheHtmlRequest(
        context: Context,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val cachePath = htmlCachePath
            val fileName = htmlCacheKey(url)
            val key = cachePath + File.separator + fileName
            val file = File(key)
            if (file.exists() && file.isFile && file.length() > 0L) {
                val age = System.currentTimeMillis() - file.lastModified()
                if (age >= HTML_HARD_EXPIRE_MS) {
                    // 硬过期：强制回源，避免后台刷新持续失败导致永久陈旧
                    file.delete()
                    scheduleHtmlDownload(key, cachePath, fileName, url, request)
                    return null
                }
                // 命中：直接返回缓存 + 后台异步刷新（stale-while-revalidate）
                scheduleHtmlDownload(key, cachePath, fileName, url, request)
                WebResourceResponse("text/html", null, file.inputStream())
            } else {
                scheduleHtmlDownload(key, cachePath, fileName, url, request)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "cacheHtmlRequest failed: ${request.url}", e)
            null
        }
    }

    /**
     * 计算 HTML 主文档的缓存 key。
     *
     * 以「URL + Cookie」的哈希作为键：当同一 URL 的内容随登录账号变化时，
     * Cookie 不同则 key 不同，避免切换账号后命中前一账号的旧页面。
     * 无 Cookie 时退化为纯 URL 哈希，保留无身份场景的缓存能力。
     */
    private fun htmlCacheKey(url: String): String {
        val cookie = cookieStore.getCookie(url).orEmpty()
        val raw = if (cookie.isBlank()) url else "$url|$cookie"
        return DigestUtils.md5Hex(raw)
    }

    override fun isCacheableResource(request: WebResourceRequest): Boolean {
        val extension = request.getExtensionFromUrl()
        if (extension.isBlank()) {
            val accept = request.requestHeaders["Accept"] ?: return false
            return accept == ACCEPT_IMAGE && request.method.equals("GET", true)
        }
        return extension in CACHEABLE_EXTENSIONS
    }

    override fun cacheResourceRequest(
        context: Context,
        request: WebResourceRequest,
        referer: String?,
    ): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val cachePath = webCachePath
            val fileName = DigestUtils.md5Hex(url)
            val key = cachePath + File.separator + fileName
            val file = File(key)
            if (file.exists() && file.isFile && file.length() > 0L) {
                // 命中：touch mtime 作为"最近访问"标记，直接返回文件流
                file.setLastModified(System.currentTimeMillis())
                val mimeType = request.getMimeTypeFromUrl()
                WebResourceResponse(mimeType, null, file.inputStream()).apply {
                    responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
                }
            } else {
                // 未命中：return null 让 WebView 走自带 HTTP 缓存，同时后台异步下载填充 web_cache
                scheduleAsyncDownload(key, file, cachePath, fileName, url, referer, request)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "cacheResourceRequest failed: ${request.url}", e)
            null
        }
    }

    override fun prefetchDns(url: String) {
        try {
            val host = URI(url).host ?: return
            AppScope.launch(Dispatchers.IO) {
                try {
                    InetAddress.getByName(host)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun evictByMtimeIfNeeded(context: Context) {
        AppScope.launch(Dispatchers.IO) {
            try {
                evictByMtime(webCachePath, WEB_CACHE_MAX_FILES)
                evictByMtime(htmlCachePath, HTML_CACHE_MAX_FILES)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 后台异步下载资源到本地磁盘缓存。
     *
     * 设计要点：
     * - 不阻塞 [shouldInterceptRequest] 的调用线程，立即返回；
     * - 通过 [inFlightDownloads] 按 key 去重，同一个资源只下载一次；
     * - 下载在 [Dispatchers.IO] 上执行，不占用主线程；
     * - 下载成功后 touch 文件 mtime 并检查是否触发按 mtime 淘汰。
     */
    private fun scheduleAsyncDownload(
        key: String,
        file: File,
        cachePath: String,
        fileName: String,
        url: String,
        referer: String?,
        request: WebResourceRequest,
    ) {
        // 组装下载请求头：透传 WebView 原始请求头 + 显式补充 Cookie / Referer。
        // 虽 OkHttp 已通过 CookieJar 同步 Cookie，但显式补充可形成双保险，
        // 并便于通过日志诊断 Cookie/Referer 是否真正生效。
        val headers = buildDownloadHeaders(url, referer, request)
        val partFileName = "$fileName.part"
        val partFile = File(cachePath, partFileName)
        val job = AppScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val result = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS.milliseconds) {
                    downloader.download(url, cachePath, partFileName, headers)
                }
                if (result?.success == true && partFile.exists() && partFile.length() > 0L) {
                    // 原子 rename：仅完整下载成功才覆盖目标，截断的 .part 不会成为缓存目标
                    if (file.exists()) file.delete()
                    partFile.renameTo(file)
                    file.setLastModified(System.currentTimeMillis())
                } else {
                    // 超时/失败：无条件删除 .part，避免半截文件残留
                    partFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Async download failed: $url", e)
                partFile.delete()
            } finally {
                // 仅当表项仍是本协程时才移除，防止被取消的协程误删正在运行的表项
                coroutineContext[Job]?.let { inFlightDownloads.remove(key, it) }
            }
        }
        // putIfAbsent 保证并发安全：仅当成功插入（无并发下载）时才启动，避免重复下载
        val existing = inFlightDownloads.putIfAbsent(key, job)
        if (existing != null) {
            job.cancel()
        } else {
            job.start()
        }
    }

    /**
     * 后台异步下载 HTML 主文档到本地磁盘缓存（stale-while-revalidate 的下载侧）。
     *
     * - 复用 [inFlightDownloads] 按 key 去重，命中刷新与未命中下载共用；
     * - 先写临时文件、成功后原子 rename 覆盖，避免下载失败损坏旧缓存；
     * - 成功后 touch mtime 作为「最近一次成功刷新时间」，供 [HTML_HARD_EXPIRE_MS] 判断。
     */
    private fun scheduleHtmlDownload(
        key: String,
        cachePath: String,
        fileName: String,
        url: String,
        request: WebResourceRequest,
    ) {
        val headers = buildDownloadHeaders(url, null, request)
        val tmpFileName = "$fileName.tmp"
        val job = AppScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val result = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS.milliseconds) {
                    downloader.download(url, cachePath, tmpFileName, headers)
                }
                val tmp = File(cachePath, tmpFileName)
                val target = File(cachePath, fileName)
                if (result?.success == true && tmp.exists() && tmp.length() > 0L) {
                    if (target.exists()) target.delete()
                    tmp.renameTo(target)
                    target.setLastModified(System.currentTimeMillis())
                    // HTML 缓存无独立淘汰时机，下载成功后同步检查并淘汰超出上限的文件
                    evictByMtime(cachePath, HTML_CACHE_MAX_FILES)
                } else {
                    tmp.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Html download failed: $url", e)
                File(cachePath, tmpFileName).delete()
            } finally {
                // 仅当表项仍是本协程时才移除，防止被取消的协程误删正在运行的表项
                coroutineContext[Job]?.let { inFlightDownloads.remove(key, it) }
            }
        }
        val existing = inFlightDownloads.putIfAbsent(key, job)
        if (existing != null) {
            job.cancel()
        } else {
            job.start()
        }
    }

    /**
     * 组装异步缓存下载的请求头。
     *
     * - 透传 [WebResourceRequest.requestHeaders]（含 User-Agent / Accept 等）；
     * - 从 [CookieManager] 显式补充 Cookie，与 OkHttp 的 CookieJar 形成双保险；
     * - 透传 [referer]（防盗链站点需要），WebView 的 requestHeaders 通常不含 Referer。
     */
    private fun buildDownloadHeaders(
        url: String,
        referer: String?,
        request: WebResourceRequest,
    ): Map<String, String> {
        val headers = HashMap<String, String>()
        request.requestHeaders.forEach { (k, v) -> headers[k] = v }
        cookieStore.getCookie(url)?.let { cookie ->
            if (cookie.isNotBlank()) headers["Cookie"] = cookie
        }
        if (!referer.isNullOrBlank()) headers["Referer"] = referer
        return headers
    }

    /**
     * 当 `web_cache` 目录下文件数超过 [WEB_CACHE_MAX_FILES] 时，按文件最后修改时间（mtime）
     * 升序删除最旧的一批文件，释放磁盘空间。
     *
     * mtime 在每次缓存命中（[cacheResourceRequest]）和下载完成（[scheduleAsyncDownload]）
     * 时被 touch 为当前时间，因此越久未用的文件 mtime 越早，天然等价于 LRU 语义。
     * 只在溢出时才执行文件系统遍历和排序，冷启动零开销。
     */
    private fun evictByMtime(cachePath: String, maxFiles: Int) {
        try {
            val dir = File(cachePath)
            val files = dir.listFiles() ?: return
            if (files.size <= maxFiles) return

            // 按 mtime 升序（最旧的在前），删除超出上限的文件
            files.sortedBy { it.lastModified() }
                .take(files.size - maxFiles)
                .forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "evictByMtime failed", e)
        }
    }
}
