package com.example.fragmject.core.webview

import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import com.example.fragmject.core.android.platform.cache.CacheDirs
import com.example.fragmject.core.android.platform.cache.CacheUtils
import com.example.fragmject.core.android.platform.file.FileUtils
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.DownloadResource
import com.example.fragmject.core.domain.session.CookieStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * WebView 资源缓存实现：本地静态资源磁盘缓存 + HTML 主文档缓存 + 加载优化。
 *
 * 缓存策略由 [WebCachePolicy] 的纯函数决定，本类只负责 IO 与生命周期：
 * - 不设主机级例外：任何主机的内容都可缓存，身份隔离改由「HTML 键含 Cookie + 资源条目分级 + 同源闸」承担；
 * - 静态资源按文件 mtime 淘汰，冷启动零开销；
 * - HTML 主文档采用 stale-while-revalidate：命中返回缓存并后台刷新，保证首屏提速的同时内容不永久陈旧。
 *
 * 与 WebView 原生 HTTP 缓存的边界（重要）：
 * `shouldInterceptRequest` 在原生缓存之前接管请求，因此**源站的缓存指令必须由我们自己执行**。
 * 落盘前的判定（`Cache-Control` / `Vary` / `Content-Type`）与命中后的响应头回填都不可省略，
 * 否则就是在用一套私有策略覆盖 Chromium 的隐私规则。
 */
@Singleton
class WebResourceCacheManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val downloader: DownloadRepository,
    private val cookieStore: CookieStore,
    private val fetcher: WebResourceFetcher,
    private val settings: WebCacheSettings,
    private val appScope: AppCoroutineScope,
) : WebResourceCache {

    companion object {
        private const val TAG = "WebResourceCache"
        private const val DOWNLOAD_TIMEOUT_MS = 8_000L

        /**
         * mtime 回写的最小间隔。
         *
         * [cacheResourceRequest] 命中时会 touch mtime 作为 LRU 依据，而 touch 是一次
         * utimes 写系统调用，发生在 `shouldInterceptRequest` 的同步路径上。
         * LRU 只需要「大致最近使用」的粒度，因此限制回写频率。
         */
        private const val MTIME_TOUCH_INTERVAL_MS = 60_000L

        /** 本地缓存文件数上限。超过时按 mtime 淘汰最旧文件，mtime 在每次命中时被 touch。 */
        private const val WEB_CACHE_MAX_FILES = 5000

        /** HTML 缓存文件数上限（不含 `.meta` 旁注文件），超过时按 mtime 淘汰最旧文件。 */
        private const val HTML_CACHE_MAX_FILES = 200

        /** HTML 硬过期时长：超过则强制回源，避免后台刷新持续失败导致内容永久陈旧。 */
        private const val HTML_HARD_EXPIRE_MS = 7L * 24 * 60 * 60 * 1000

        /**
         * HTML 最小刷新间隔：命中缓存且距上次**成功刷新**不足该间隔时，直接返回缓存，
         * 不再发起后台刷新。
         *
         * mtime 在命中时不会被 touch、只在下载成功时写入（见 [scheduleHtmlDownload]），
         * 因此 mtime 即「上次成功刷新时间」，可直接用作间隔判断。
         * 没有这层判断时，1 分钟内重复打开同一文章会触发 N 次完整 HTML 下载。
         */
        private const val HTML_MIN_REFRESH_INTERVAL_MS = 5L * 60 * 1000

        /** HTML 缓存条目的旁注文件后缀：记录 Content-Type（含 charset）。 */
        private const val META_SUFFIX = ".meta"

        /** HTML 下载中的临时文件后缀（静态资源的临时文件由 [WebResourceFetcher] 管理）。 */
        private const val HTML_TMP_SUFFIX = ".tmp"

        private const val META_KEY_CONTENT_TYPE = "contentType"
        private const val DEFAULT_HTML_MIME = "text/html"
    }

    /** 正在下载中的缓存任务，按缓存 key 去重，避免并发重复下载同一资源。 */
    private val inFlightDownloads: ConcurrentHashMap<String, Job> = ConcurrentHashMap()

    /**
     * 静态资源缓存目录（解析一次）。
     *
     * 每次拦截都调 CacheUtils.getDirPath 会执行 mkdirs 并产生多轮磁盘 stat，
     * 叠加在 shouldInterceptRequest 线程上拖慢首屏，故在此缓存。
     */
    private val webCachePath: String by lazy { CacheUtils.getDirPath(appContext, CacheDirs.WEB) }

    /**
     * HTML 主文档缓存目录，固定落在 internal `cacheDir`。
     *
     * 与静态资源不同：主文档内容可能含个人信息（第三方站点同样可能带账号态），
     * 而 `externalCacheDir` 在部分设备上对 shell / 备份工具可读，保护强度低于 internal storage。
     */
    private val htmlCachePath: String by lazy {
        CacheUtils.getInternalDirPath(appContext, CacheDirs.WEB_HTML)
    }

    init {
        // 用户关闭「登录态缓存」后，已落盘的带凭据条目要立即清掉：
        // 否则开关看似生效、磁盘上却仍留着上一账号的图片。
        appScope.launch(Dispatchers.IO) {
            settings.credentialedCachingEnabled
                .drop(1)
                .filter { enabled -> !enabled }
                .collect { fetcher.purgeCredentialed(webCachePath) }
        }
    }

    override fun isCacheableHtml(request: WebResourceRequest): Boolean =
        WebCachePolicy.isCacheableHtml(
            method = request.method,
            isForMainFrame = request.isForMainFrame,
            url = request.url.toString(),
            accept = request.headerValue("Accept"),
        )

    override fun cacheHtmlRequest(
        context: Context,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val cachePath = htmlCachePath
            val fileName = WebCachePolicy.htmlCacheKey(url, cookieStore.getCookie(url))
            val file = File(cachePath, fileName)
            if (!file.isFile || file.length() <= 0L) {
                // 无缓存或是残留的空文件：回源，同时后台填充
                if (file.exists()) file.delete()
                scheduleHtmlDownload(cachePath, fileName, url, request)
                return null
            }
            val age = System.currentTimeMillis() - file.lastModified()
            if (age >= HTML_HARD_EXPIRE_MS) {
                // 硬过期：强制回源，避免后台刷新持续失败导致永久陈旧
                file.delete()
                metaFile(cachePath, fileName).delete()
                scheduleHtmlDownload(cachePath, fileName, url, request)
                return null
            }
            // 距上次成功刷新过近时不再后台刷新，否则短时重复访问会各自触发一次完整下载
            if (age >= HTML_MIN_REFRESH_INTERVAL_MS) {
                scheduleHtmlDownload(cachePath, fileName, url, request)
            }
            buildHtmlResponse(cachePath, fileName, file)
        } catch (e: Exception) {
            Log.e(TAG, "cacheHtmlRequest failed: ${request.url}", e)
            null
        }
    }

    /**
     * 用缓存文件构造响应：回填 `Content-Type`（含 charset）。
     *
     * 这一步不是可选优化——缓存命中时原始响应头已丢失，缺 charset 会让 GBK 等非 UTF-8 页面乱码。
     * 同时**不回放 `Set-Cookie`**：缓存页不应写 Cookie，否则可能复活上一会话的凭证。
     */
    private fun buildHtmlResponse(cachePath: String, fileName: String, file: File): WebResourceResponse {
        val parsed = WebCachePolicy.parseContentType(readMetaContentType(cachePath, fileName))
        val mimeType = parsed.mimeType.ifEmpty { DEFAULT_HTML_MIME }
        return WebResourceResponse(mimeType, parsed.charset, file.inputStream())
    }

    override fun isCacheableResource(request: WebResourceRequest): Boolean =
        WebCachePolicy.isCacheableResource(
            method = request.method,
            extension = request.getExtensionFromUrl(),
            accept = request.headerValue("Accept"),
        )

    override fun cacheResourceRequest(
        context: Context,
        request: WebResourceRequest,
        referer: String?,
    ): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val cachePath = webCachePath
            val file = firstCachedEntry(cachePath, url, referer, request)
            if (file == null) {
                // 未命中：return null 让 WebView 走自带 HTTP 缓存，同时后台异步填充
                scheduleResourceDownload(cachePath, url, referer, request)
                return null
            }
            // 命中：仅在距上次 touch 超过 [MTIME_TOUCH_INTERVAL_MS] 时才回写 mtime。
            // 原实现每次命中都写一次 utimes，100 资源的页面就是 100 次同步写系统调用。
            val now = System.currentTimeMillis()
            if (now - file.lastModified() > MTIME_TOUCH_INTERVAL_MS) {
                file.setLastModified(now)
            }
            buildResourceResponse(request, file)
        } catch (e: Exception) {
            Log.e(TAG, "cacheResourceRequest failed: ${request.url}", e)
            null
        }
    }

    /**
     * 找出可命中的缓存条目。
     *
     * 带凭据条目（`.cred`）只在**同源**请求下参与命中：内容可能属于当前账号，
     * 交给其他站点读取就是跨站泄露。匿名条目与身份无关，任何来源都可命中。
     *
     * 同源时优先取带凭据条目（更贴近当前账号），没有再退到匿名条目。
     */
    private fun firstCachedEntry(
        cachePath: String,
        url: String,
        referer: String?,
        request: WebResourceRequest,
    ): File? {
        val sameHost = WebCachePolicy.isSameHost(referer ?: request.headerValue("Origin"), url)
        return WebCachePolicy.resourceEntryNames(url, sameHost)
            .asSequence()
            .map { File(cachePath, it) }
            .firstOrNull { it.isFile && it.length() > 0L }
    }

    /**
     * 用缓存文件构造静态资源响应。
     *
     * 命中时原始响应头已丢失，必须补回 CORS 头，否则 `crossorigin` 图片 / 跨域 `@font-face`
     * 会被浏览器判为 CORS 失败（页面表现为图片一直转圈）。
     *
     * 口径见 [WebCachePolicy.corsHeaders]：识别出合法 `Origin` 就精确回显，识别不出来（缺失 /
     * `null` / 非法值）降级为 `*`——**fail-open**。因为 `Origin` 是否出现在拦截层属 WebView 实现细节，
     * 按「看不到就当非 CORS」处理会重新踩回「二次进入图片转圈」的坑。
     */
    private fun buildResourceResponse(request: WebResourceRequest, file: File): WebResourceResponse {
        val response = WebResourceResponse(request.getMimeTypeFromUrl(), null, file.inputStream())
        response.responseHeaders = WebCachePolicy.corsHeaders(request.headerValue("Origin"))
        return response
    }

    override fun prefetchDns(url: String) {
        try {
            val host = URI(url).host ?: return
            appScope.launch(Dispatchers.IO) {
                try {
                    InetAddress.getByName(host)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun evictByMtimeIfNeeded(context: Context) {
        appScope.launch(Dispatchers.IO) {
            try {
                // 旧版本把 HTML 缓存放在 externalCacheDir；改到 internal 后遗留目录不再被读取，
                // 主动删除，避免升级后用户设备上一直留着一份明文网页缓存
                appContext.externalCacheDir?.let { FileUtils.delete(File(it, CacheDirs.WEB_HTML)) }
                evictByMtime(webCachePath, WEB_CACHE_MAX_FILES)
                evictByMtime(htmlCachePath, HTML_CACHE_MAX_FILES)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 清除全部本地缓存（含在途下载），用于登出 / 换号等会话变更。
     *
     * 先取消在途任务再删目录：否则已发出的下载会在删除后把响应重新写回磁盘，
     * 清缓存变成「清了个寂寞」。目录随后续写入按需重建，这里立即重建以保持
     * [webCachePath] / [htmlCachePath] 这类 `by lazy` 路径一直可用。
     */
    override suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        inFlightDownloads.values.forEach { it.cancel() }
        inFlightDownloads.clear()
        // 主机结论属于上一个会话：换号后必须重新探测，否则会把「需要登录」错记成匿名
        fetcher.reset()
        FileUtils.delete(File(webCachePath))
        FileUtils.delete(File(htmlCachePath))
        CacheUtils.getDirFile(appContext, CacheDirs.WEB)
        CacheUtils.getInternalDirFile(appContext, CacheDirs.WEB_HTML)
    }

    /**
     * 后台填充一个静态资源。
     *
     * 具体策略（匿名优先 / 失败带凭据重试 / 按主机记忆结论）由 [WebResourceFetcher] 承担；
     * 本类只负责按 URL 去重、不阻塞 `shouldInterceptRequest` 线程，以及与 HTML 缓存共用生命周期。
     */
    private fun scheduleResourceDownload(
        cachePath: String,
        url: String,
        referer: String?,
        request: WebResourceRequest,
    ) {
        val headers = request.requestHeaders
        val host = request.url.host.orEmpty()
        val key = cacheKey(cachePath, WebCachePolicy.resourceEntryName(url, credentialed = false))
        val job = appScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                fetcher.fetch(cachePath, url, referer, headers, host)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "resource fill failed: $url", e)
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
     * 后台异步下载 HTML 主文档（stale-while-revalidate 的下载侧）。
     *
     * - 复用 [inFlightDownloads] 按 key 去重，命中刷新与未命中下载共用；
     * - 先写临时文件、判定通过后原子 rename，避免下载失败损坏旧缓存，也避免源站
     *   标记为不可缓存的响应（`no-store` / `private` / 无法复现的 `Vary`）落盘；
     * - 成功后 touch mtime 作为「最近一次成功刷新时间」，供硬过期判断；
     * - `Content-Type` 写入 `.meta` 旁注，命中时回填 mime + charset。
     */
    private fun scheduleHtmlDownload(
        cachePath: String,
        fileName: String,
        url: String,
        request: WebResourceRequest,
    ) {
        val headers = htmlFillHeaders(url, request)
        val tmpFileName = fileName + HTML_TMP_SUFFIX
        val tmpFile = File(cachePath, tmpFileName)
        val job = appScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val resource = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS.milliseconds) {
                    downloader.downloadResource(url, cachePath, tmpFileName, headers)
                }
                val downloaded = resource.takeIfDownloaded(tmpFile)
                if (downloaded == null || !isStorableHtml(downloaded.responseHeaders)) {
                    tmpFile.delete()
                    return@launch
                }
                val target = File(cachePath, fileName)
                if (target.exists()) target.delete()
                if (!tmpFile.renameTo(target)) {
                    tmpFile.delete()
                    return@launch
                }
                target.setLastModified(System.currentTimeMillis())
                writeMetaContentType(cachePath, fileName, downloaded.responseHeaders.headerValue("Content-Type"))
                // HTML 缓存无独立淘汰时机，下载成功后同步检查并淘汰超出上限的文件
                evictByMtime(cachePath, HTML_CACHE_MAX_FILES)
            } catch (e: Exception) {
                Log.e(TAG, "Html download failed: $url", e)
                tmpFile.delete()
            } finally {
                // 仅当表项仍是本协程时才移除，防止被取消的协程误删正在运行的表项
                coroutineContext[Job]?.let { inFlightDownloads.remove(cacheKey(cachePath, fileName), it) }
            }
        }
        val existing = inFlightDownloads.putIfAbsent(cacheKey(cachePath, fileName), job)
        if (existing != null) {
            job.cancel()
        } else {
            job.start()
        }
    }

    /** 下载产出了可用的完整文件时返回结果，否则返回 null。 */
    private fun DownloadResource?.takeIfDownloaded(file: File): DownloadResource? =
        this?.takeIf { it.result.success && file.isFile && file.length() > 0L }

    /** HTML 落盘闸门：源站策略 + 必须是 HTML 文档。 */
    private fun isStorableHtml(headers: Map<String, String>): Boolean =
        WebCachePolicy.allowsStorage(
            cacheControl = headers.headerValue("Cache-Control"),
            vary = headers.headerValue("Vary"),
        ) && WebCachePolicy.isStorableHtml(headers.headerValue("Content-Type"))

    /**
     * HTML 主文档的缓存填充请求头。
     *
     * 与资源不同，HTML 允许带 Cookie：它的缓存键含 Cookie（[WebCachePolicy.htmlCacheKey]），
     * 本身就是**按身份隔离**的，不存在「匿名条目被跨源复用」的问题。
     * 请求头仍走白名单（[WebCachePolicy.cacheFillHeaders]），不再原样透传全部字段。
     */
    private fun htmlFillHeaders(url: String, request: WebResourceRequest): Map<String, String> =
        WebCachePolicy.cacheFillHeaders(
            source = request.requestHeaders,
            referer = null,
            cookie = cookieStore.getCookie(url)?.takeIf { it.isNotBlank() },
        )

    private fun cacheKey(cachePath: String, fileName: String): String =
        cachePath + File.separator + fileName

    private fun metaFile(cachePath: String, fileName: String): File =
        File(cachePath, fileName + META_SUFFIX)

    private fun writeMetaContentType(cachePath: String, fileName: String, contentType: String?) {
        // 写入失败只影响 charset 回填，不影响缓存本身是否可用
        runCatching {
            val meta = Properties().apply { setProperty(META_KEY_CONTENT_TYPE, contentType.orEmpty()) }
            metaFile(cachePath, fileName).outputStream().use { meta.store(it, null) }
        }.onFailure { Log.w(TAG, "writeMeta failed: $fileName", it) }
    }

    private fun readMetaContentType(cachePath: String, fileName: String): String? = runCatching {
        val meta = metaFile(cachePath, fileName)
        if (!meta.isFile) {
            null
        } else {
            Properties().apply { meta.inputStream().use { load(it) } }
                .getProperty(META_KEY_CONTENT_TYPE)
                ?.takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    /**
     * 当目录下文件数超过 [maxFiles] 时，按文件最后修改时间（mtime）升序删除最旧的一批文件。
     *
     * mtime 在每次缓存命中或下载完成时被 touch 为当前时间，因此越久未用的文件 mtime 越早，
     * 天然等价于 LRU 语义。只在溢出时才执行文件系统遍历和排序，冷启动零开销。
     *
     * `.meta` 旁注不单独计数：随所属数据文件一起删除，避免「数据没了、旁注还在」的孤儿堆积。
     */
    private fun evictByMtime(cachePath: String, maxFiles: Int) {
        try {
            val files = File(cachePath).listFiles()
                ?.filterNot { it.name.endsWith(META_SUFFIX) }
                ?: return
            if (files.size <= maxFiles) return

            // 先一次性取出 mtime 再排序：sortedBy 的选择器会在**每次比较**时重新调用，
            // 直接写 sortedBy { it.lastModified() } 会产生 O(n log n) 次 stat
            // （5000 文件约 6 万次）。按 mtime 升序（最旧的在前），删除超出上限的文件。
            val withMtime = files.map { it to it.lastModified() }
            withMtime.sortedBy { it.second }
                .take(withMtime.size - maxFiles)
                .forEach { (file, _) ->
                    file.delete()
                    metaFile(cachePath, file.name).delete()
                }
        } catch (e: Exception) {
            Log.e(TAG, "evictByMtime failed", e)
        }
    }
}
