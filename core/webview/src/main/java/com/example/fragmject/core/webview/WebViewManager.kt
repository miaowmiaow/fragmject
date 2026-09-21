package com.example.fragmject.core.webview

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.graphics.Color
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.fragmject.core.android.platform.AppScope
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.android.platform.CacheUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * WebView 管理器：
 * - 维护一个空闲 WebView 实例做热身复用，使用 [MutableContextWrapper] 在 Activity 之间安全切换 baseContext，
 *   不再以 url 为 key 持有多个实例，避免内存堆积与回调闭包泄漏。
 * - 维护本地静态资源（图片/样式/脚本/字体）的磁盘缓存，按文件 mtime 淘汰，冷启动零开销。
 */
@SuppressLint("SetJavaScriptEnabled")
@Singleton
class WebViewManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val downloadRepository: DownloadRepository,
) : WebViewPool {

    companion object {
        private const val TAG = "WebViewManager"
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
    }

    /**
     * 空闲位：当前最多缓存一个"未与任何 url 绑定"的 WebView 实例，用于下次 obtain 时的热身复用，
     * 节省首屏 WebView 初始化耗时。
     *
     * 静态字段持有 WebView 看似存在 Context 泄漏风险，但本类的不变式是：
     * 放入此字段的 WebView 其 [MutableContextWrapper] 的 baseContext 一定已经被切回 ApplicationContext
     * （见 [warmupSpareWebView]），因此不会真的泄漏 Activity。
     */
    @SuppressLint("StaticFieldLeak")
    private var spareWebView: WebView? = null

    /**
     * keep-alive 池容量，按设备内存等级自适应（低端 2 / 中端 4 / 高端 8），
     * 避免低端机因常驻过多 WebView（单实例 30~80MB）而内存吃紧。
     */
    private val keepAliveCapacity: Int = calculateKeepAliveCapacity(appContext)

    /**
     * keep-alive 池：以 url 为 key 缓存最近使用的 WebView 实例（含其内部状态），实现
     * "详情页返回时不重新加载、不丢失操作"。
     *
     * 使用 [LinkedHashMap] 的 access-order 模式天然形成 LRU；超过 [keepAliveCapacity] 时
     * 淘汰最久未用的实例并 destroy。同样通过 [isSafeForSparePool] 不变式保证不会泄漏 Activity。
     */
    @SuppressLint("StaticFieldLeak")
    private val keepAlivePool: LinkedHashMap<String, WebView> =
        LinkedHashMap(keepAliveCapacity, 0.75f, true)

    /** 正在下载中的缓存任务，按缓存 key 去重，避免并发重复下载同一资源。 */
    private val inFlightDownloads: ConcurrentHashMap<String, Job> = ConcurrentHashMap()

    private fun create(context: Context): WebView {
        // 始终以 MutableContextWrapper 作为 baseContext，便于在 Activity 间切换而不持有 Activity 引用
        val wrapper = MutableContextWrapper(context)
        val webView = WebView(wrapper)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        val webSettings = webView.settings
        webSettings.setSupportZoom(true)
        webSettings.allowFileAccess = false
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.domStorageEnabled = true
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.displayZoomControls = false
        webSettings.useWideViewPort = true
        webSettings.mediaPlaybackRequiresUserGesture = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        webView.setRendererPriorityPolicy(
            WebView.RENDERER_PRIORITY_BOUND,
            true
        )
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        return webView
    }

    /**
     * 应用启动后调用：
     * 用 ApplicationContext 预创建一个空闲 WebView，下次 obtain 直接复用，省掉首屏 WebView 初始化耗时。
     */
    override fun prepare(context: Context) {
        val appCtx = context.applicationContext
        Handler(appCtx.mainLooper).postDelayed({
            warmupSpareWebView(appCtx)
        }, 500L)
    }

    private fun warmupSpareWebView(appContext: Context) {
        if (spareWebView == null) {
            try {
                // 用 ApplicationContext 创建，保证空闲实例从一开始就不持有 Activity
                spareWebView = create(appContext.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "warmupSpareWebView failed", e)
            }
        }
    }

    /**
     * 获取一个 WebView 实例。优先级：
     * 1. keep-alive 池中存在与 [url] 完全匹配的实例 —— 命中后直接复用，**保留页面内部状态**
     *    （滚动位置、表单输入、JS 上下文等），用户从详情页返回时无需重新加载；
     * 2. 空闲位中的预热实例；
     * 3. 全新创建。
     */
    override fun obtain(context: Context, url: String): WebView {
        // 进入 WebView 页面时异步检查缓存是否超出软上限，避免主线程文件 I/O
        AppScope.launch(Dispatchers.IO) {
            try {
                evictByMtimeIfNeeded(
                    CacheUtils.getDirPath(
                        context.applicationContext,
                        WEB_CACHE_DIR
                    )
                )
            } catch (_: Exception) {
            }
        }

        val cached = keepAlivePool.remove(url)
        val webView: WebView
        val reuseFromKeepAlive: Boolean
        if (cached != null) {
            webView = cached
            reuseFromKeepAlive = true
        } else {
            webView = spareWebView?.also {
                spareWebView = null
                // 取走后异步补充一个新的热身实例，保证下次 obtain 仍能秒开
                val appCtx = it.context.applicationContext
                AppScope.launch(Dispatchers.Main) {
                    if (spareWebView == null) {
                        try {
                            spareWebView = create(appCtx)
                        } catch (e: Exception) {
                            Log.e(TAG, "autoFillSpare failed", e)
                        }
                    }
                }
            } ?: create(context)
            reuseFromKeepAlive = false
        }
        (webView.context as? MutableContextWrapper)?.baseContext = context
        if (webView.parent != null) {
            (webView.parent as ViewGroup).removeView(webView)
        }
        // 复用前清掉残留的回调闭包，避免上一个页面的 client 污染当前页
        webView.tag = null
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
        webView.setOnLongClickListener(null)
        webView.setDownloadListener(null)
        if (!reuseFromKeepAlive && webView.url != url) {
            // 仅在"非 keep-alive 复用"路径上才清历史；keep-alive 命中时保留 WebView 现有状态
            webView.stopLoading()
            webView.clearHistory()
        }
        return webView
    }

    /**
     * 回收 WebView：
     * - 解绑所有 Activity/Composable 相关的回调闭包，避免长期持有；
     * - 把 [MutableContextWrapper] 的 baseContext 切回 ApplicationContext；
     * - 若 WebView 当前 url 有效，则进入 keep-alive 池保留页面状态（滚动位置/表单/JS 上下文），
     *   下次同 url obtain 时直接复用，不再重新加载；
     * - 否则放回空闲位（仅一席）作为下次新页面的热身实例；
     * - 池满或不满足安全不变式时直接 destroy。
     */
    override fun recycle(webView: WebView) {
        try {
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.setOnLongClickListener(null)
            webView.setDownloadListener(null)
            webView.tag = null
            if (webView.parent != null) {
                (webView.parent as ViewGroup).removeView(webView)
            }
            (webView.context as? MutableContextWrapper)?.let { wrapper ->
                wrapper.baseContext = wrapper.baseContext.applicationContext
            }
            if (!isSafeForSparePool(webView)) {
                webView.stopLoading()
                webView.removeAllViews()
                webView.destroy()
                return
            }
            val currentUrl = webView.url
            if (!currentUrl.isNullOrBlank() && currentUrl != "about:blank") {
                // 关键：不再 loadUrl("about:blank")、不再清历史，原样保留 WebView 状态进入 keep-alive 池
                val previous = keepAlivePool.put(currentUrl, webView)
                if (previous != null && previous !== webView) {
                    // 同 url 旧实例淘汰
                    destroyQuietly(previous)
                }
                trimKeepAlivePool()
            } else if (spareWebView == null) {
                webView.stopLoading()
                spareWebView = webView
            } else {
                webView.stopLoading()
                webView.removeAllViews()
                webView.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "recycle failed", e)
        }
    }

    /** 超过容量时淘汰最久未访问的 WebView 实例。 */
    private fun trimKeepAlivePool() {
        while (keepAlivePool.size > keepAliveCapacity) {
            val iterator = keepAlivePool.entries.iterator()
            if (!iterator.hasNext()) break
            val eldest = iterator.next().value
            iterator.remove()
            destroyQuietly(eldest)
        }
    }

    private fun destroyQuietly(webView: WebView) {
        try {
            if (webView.parent != null) {
                (webView.parent as ViewGroup).removeView(webView)
            }
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.removeAllViews()
            webView.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "destroyQuietly failed", e)
        }
    }

    /**
     * 校验 WebView 当前的 baseContext 是否已切回 ApplicationContext，仅当满足该不变式时才允许进入空闲池。
     * 这是防止意外引入 Activity 引用的最后一道兜底。
     */
    private fun isSafeForSparePool(webView: WebView): Boolean {
        val ctx = webView.context
        val base = if (ctx is ContextWrapper) ctx.baseContext else ctx
        return base === base.applicationContext
    }

    override fun releaseAll() {
        try {
            spareWebView?.let { destroyQuietly(it) }
            spareWebView = null
            keepAlivePool.values.forEach { destroyQuietly(it) }
            keepAlivePool.clear()
        } catch (e: Exception) {
            Log.e(TAG, "releaseAll failed", e)
        }
    }

    /**
     * 中等内存压力时调用：清空 keep-alive 池中所有缓存的 WebView，仅保留一个空闲热身实例。
     *
     * 主要应对 onTrimMemory 的 TRIM_MEMORY_BACKGROUND / TRIM_MEMORY_RUNNING_LOW 等档位：
     * 此时仍有继续使用的可能，因此保留 spare 让下一次 obtain 仍可秒开；
     * 但已经堆积的 keep-alive 实例（每个约 30~80MB）应当主动释放，给系统腾出内存。
     */
    override fun trimToSpare() {
        try {
            if (keepAlivePool.isEmpty()) return
            // 拷贝一份再清，避免遍历期间结构修改
            val snapshot = keepAlivePool.values.toList()
            keepAlivePool.clear()
            snapshot.forEach { destroyQuietly(it) }
        } catch (e: Exception) {
            Log.e(TAG, "trimToSpare failed", e)
        }
    }

    /**
     * 按设备内存等级计算 keep-alive 池容量：
     * - 低端（<256MB）：2 个，峰值约 160MB；
     * - 中端（256~511MB）：4 个，峰值约 320MB；
     * - 高端（≥512MB）：8 个，峰值约 640MB。
     *
     * 仅在前台内存充裕时才可能占满；一旦触发 onTrimMemory 会立即 trimToSpare/releaseAll 释放。
     */
    private fun calculateKeepAliveCapacity(context: Context): Int {
        val memoryClass =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
        return when {
            memoryClass < 256 -> 2
            memoryClass < 512 -> 4
            else -> 8
        }
    }

    override fun isCacheResource(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        // 忽略掉百度统计
        if (url.contains("hm.baidu.com/hm.gif")) return false
        val extension = request.getExtensionFromUrl()
        if (extension.isBlank()) {
            val accept = request.requestHeaders["Accept"] ?: return false
            return accept == ACCEPT_IMAGE && request.method.equals("GET", true)
        }
        return extension in CACHEABLE_EXTENSIONS
    }

    override fun cacheResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val cachePath = CacheUtils.getDirPath(context, WEB_CACHE_DIR)
            val fileName = url.encodeUtf8().md5().hex()
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
                scheduleAsyncDownload(key, file, cachePath, fileName, url, request)
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
        request: WebResourceRequest,
    ) {
        val job = AppScope.launch(Dispatchers.IO) {
            try {
                val result = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS.milliseconds) {
                    downloadRepository.download(url, cachePath, fileName, request.requestHeaders)
                }
                if (result != null && result.success) {
                    if (file.exists() && file.isFile && file.length() > 0L) {
                        file.setLastModified(System.currentTimeMillis())
                    }
                } else {
                    // 下载失败，清理零字节残留
                    if (file.exists() && file.length() == 0L) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Async download failed: $url", e)
                if (file.exists() && file.length() == 0L) {
                    file.delete()
                }
            } finally {
                inFlightDownloads.remove(key)
            }
        }
        // putIfAbsent 保证并发安全：如果已有协程在下载同一资源，取消刚创建的
        val existing = inFlightDownloads.putIfAbsent(key, job)
        if (existing != null) {
            job.cancel()
        }
    }

    /**
     * 当 `web_cache` 目录下文件数超过 [WEB_CACHE_MAX_FILES] 时，按文件最后修改时间（mtime）
     * 升序删除最旧的一批文件，释放磁盘空间。
     *
     * mtime 在每次缓存命中（[cacheResourceRequest]）和下载完成（[scheduleAsyncDownload]）
     * 时被 touch 为当前时间，因此越久未用的文件 mtime 越早，天然等价于 LRU 语义。
     * 只在溢出时才执行文件系统遍历和排序，冷启动零开销。
     */
    private fun evictByMtimeIfNeeded(cachePath: String) {
        try {
            val dir = File(cachePath)
            val files = dir.listFiles() ?: return
            if (files.size <= WEB_CACHE_MAX_FILES) return

            // 按 mtime 升序（最旧的在前），删除超出上限的文件
            files.sortedBy { it.lastModified() }
                .take(files.size - WEB_CACHE_MAX_FILES)
                .forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "evictByMtime failed", e)
        }
    }

    private fun WebResourceRequest.getExtensionFromUrl(): String {
        return try {
            MimeTypeMap.getFileExtensionFromUrl(url.toString())
        } catch (e: Exception) {
            Log.e(TAG, "getExtensionFromUrl failed: $url", e)
            ""
        }
    }

    private fun WebResourceRequest.getMimeTypeFromUrl(): String {
        return try {
            when (val extension = getExtensionFromUrl()) {
                "", "null", "*/*" -> "*/*"
                "json" -> "application/json"
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMimeTypeFromUrl failed: $url", e)
            "*/*"
        }
    }
}