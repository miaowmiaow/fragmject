package com.example.fragment.project.ui.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.graphics.Color
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.miaow.base.http.download
import com.example.miaow.base.utils.CacheUtils
import com.example.miaow.base.utils.LRUCache
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.time.Duration.Companion.milliseconds

/**
 * WebView 管理器：
 * - 维护一个空闲 WebView 实例做热身复用，使用 [MutableContextWrapper] 在 Activity 之间安全切换 baseContext，
 *   不再以 url 为 key 持有多个实例，避免内存堆积与回调闭包泄漏。
 * - 维护本地静态资源（图片/样式/脚本/字体）的磁盘 LRU。
 */
@SuppressLint("SetJavaScriptEnabled")
class WebViewManager private constructor() {

    companion object {
        private const val TAG = "WebViewManager"
        private const val DOWNLOAD_TIMEOUT_MS = 8_000L
        private const val WEB_CACHE_DIR = "web_cache"
        private const val WEB_CACHE_LRU_CAPACITY = 5000

        /**
         * keep-alive 池上限：保留最近使用的若干个 WebView 实例（含其内部状态：滚动位置/表单/JS
         * 上下文等），用户从详情页返回上一级时直接复用，避免重新加载页面与丢失操作。
         *
         * 取 4 是经验权衡：单个 WebView 内存占用约 30-80MB，4 个足以覆盖常见的两到三层导航深度，
         * 同时不会显著增加 OOM 风险。
         */
        private const val KEEP_ALIVE_CAPACITY = 4
        private const val ACCEPT_IMAGE =
            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

        private val CACHEABLE_EXTENSIONS = setOf(
            "ico", "bmp", "gif", "jpeg", "jpg", "png", "svg", "webp",
            "css", "js", "json",
            "eot", "otf", "ttf", "woff"
        )

        /**
         * 单例实例。
         *
         * Lint 会因为本类间接持有 [spareWebView]（WebView 内部含 Context）而报 StaticFieldLeak，
         * 但本类已通过以下不变式保证不会泄漏 Activity：
         * - [spareWebView] 始终以 ApplicationContext 创建（见 [warmupSpareWebView]）；
         * - [recycle] 时把 [MutableContextWrapper.baseContext] 切回 ApplicationContext，并由
         *   [isSafeForSparePool] 兜底校验后才允许进入空闲池。
         * 因此此处显式抑制 Lint 告警。
         */
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: WebViewManager? = null

        private fun getInstance() = INSTANCE ?: synchronized(WebViewManager::class.java) {
            INSTANCE ?: WebViewManager().also { INSTANCE = it }
        }

        fun prepare(context: Context) = getInstance().prepare(context)

        fun destroy() = getInstance().destroy()

        /**
         * 内存吃紧时调用：清空 keep-alive 池，仅保留一个空闲 [spareWebView]。
         * 与 [destroy] 的区别在于会保留热身位以便后续 obtain 仍能秒开。
         */
        fun trimToSpare() = getInstance().trimToSpare()

        /** 系统极端缺内存时调用：彻底释放所有 WebView，等价于 [destroy]，对外语义更清晰。 */
        fun releaseAll() = getInstance().destroy()

        fun obtain(context: Context, url: String): WebView = getInstance().obtain(context, url)

        fun recycle(webView: WebView) = getInstance().recycle(webView)

        fun isAssetsResource(request: WebResourceRequest): Boolean =
            getInstance().isAssetsResource(request)

        fun isCacheResource(request: WebResourceRequest): Boolean =
            getInstance().isCacheResource(request)

        fun assetsResourceRequest(
            context: Context,
            request: WebResourceRequest
        ): WebResourceResponse? = getInstance().assetsResourceRequest(context, request)

        fun cacheResourceRequest(
            context: Context,
            request: WebResourceRequest
        ): WebResourceResponse? = getInstance().cacheResourceRequest(context, request)
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
     * keep-alive 池：以 url 为 key 缓存最近使用的 WebView 实例（含其内部状态），实现
     * "详情页返回时不重新加载、不丢失操作"。
     *
     * 使用 [LinkedHashMap] 的 access-order 模式天然形成 LRU；超过 [KEEP_ALIVE_CAPACITY] 时
     * 淘汰最久未用的实例并 destroy。同样通过 [isSafeForSparePool] 不变式保证不会泄漏 Activity。
     */
    @SuppressLint("StaticFieldLeak")
    private val keepAlivePool: LinkedHashMap<String, WebView> =
        LinkedHashMap(KEEP_ALIVE_CAPACITY, 0.75f, true)

    /** 本地资源磁盘 LRU 索引。 */
    private val lruCache: LRUCache<String, String> = LRUCache(WEB_CACHE_LRU_CAPACITY)

    private fun create(context: Context): WebView {
        // 始终以 MutableContextWrapper 作为 baseContext，便于在 Activity 间切换而不持有 Activity 引用
        val wrapper = MutableContextWrapper(context)
        val webView = WebView(wrapper)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        val webSettings = webView.settings
        webSettings.setSupportZoom(true)
        webSettings.allowFileAccess = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.domStorageEnabled = true
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.displayZoomControls = false
        webSettings.useWideViewPort = true
        webSettings.mediaPlaybackRequiresUserGesture = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        return webView
    }

    /**
     * 应用启动后调用：
     * 1. 在主线程空闲时构建本地资源 LRU 索引（按创建时间排序，避免常用文件被先淘汰）。
     * 2. 用 ApplicationContext 预创建一个空闲 WebView，下次 obtain 直接复用，省掉首屏 WebView 初始化耗时。
     */
    private fun prepare(context: Context) {
        val appContext = context.applicationContext
        Looper.myQueue().addIdleHandler {
            warmupCacheIndex(appContext)
            warmupSpareWebView(appContext)
            false
        }
    }

    private fun warmupCacheIndex(context: Context) {
        try {
            val cachePath = CacheUtils.getDirPath(context, WEB_CACHE_DIR)
            File(cachePath).takeIf { it.isDirectory }?.listFiles()
                ?.sortedWith(compareByDescending {
                    // 文件创建时间越久说明使用频率越高，倒序排序避免高频文件初始化时位于队首被先淘汰
                    val attrs = Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                    attrs.creationTime().toMillis()
                })
                ?.forEach {
                    val absolutePath = it.absolutePath
                    lruCache.put(absolutePath, absolutePath)?.let { evicted ->
                        File(evicted).delete()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "warmupCacheIndex failed", e)
        }
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
    private fun obtain(context: Context, url: String): WebView {
        val cached = keepAlivePool.remove(url)
        val webView: WebView
        val reuseFromKeepAlive: Boolean
        if (cached != null) {
            webView = cached
            reuseFromKeepAlive = true
        } else {
            webView = spareWebView?.also { spareWebView = null } ?: create(context)
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
    private fun recycle(webView: WebView) {
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
        while (keepAlivePool.size > KEEP_ALIVE_CAPACITY) {
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

    private fun destroy() {
        try {
            spareWebView?.let { destroyQuietly(it) }
            spareWebView = null
            keepAlivePool.values.forEach { destroyQuietly(it) }
            keepAlivePool.clear()
        } catch (e: Exception) {
            Log.e(TAG, "destroy failed", e)
        }
    }

    /**
     * 中等内存压力时调用：清空 keep-alive 池中所有缓存的 WebView，仅保留一个空闲热身实例。
     *
     * 主要应对 onTrimMemory 的 TRIM_MEMORY_BACKGROUND / TRIM_MEMORY_RUNNING_LOW 等档位：
     * 此时仍有继续使用的可能，因此保留 spare 让下一次 obtain 仍可秒开；
     * 但已经堆积的 keep-alive 实例（每个约 30~80MB）应当主动释放，给系统腾出内存。
     */
    private fun trimToSpare() {
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

    fun isAssetsResource(request: WebResourceRequest): Boolean {
        return request.url.toString().startsWith("file:///android_asset/")
    }

    fun isCacheResource(request: WebResourceRequest): Boolean {
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

    fun assetsResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val filename = url.substringAfterLast("/")
            val suffix = url.substringAfterLast(".")
            val mimeType = request.getMimeTypeFromUrl()
            val encoding = context.assets.open(suffix + File.separator + filename)
            WebResourceResponse(mimeType, null, encoding).apply {
                responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
            }
        } catch (e: Exception) {
            Log.e(TAG, "assetsResourceRequest failed: ${request.url}", e)
            null
        }
    }

    fun cacheResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val cachePath = CacheUtils.getDirPath(context, WEB_CACHE_DIR)
            val fileName = url.encodeUtf8().md5().hex()
            val key = cachePath + File.separator + fileName
            val file = File(key)
            if (!file.exists() || !file.isFile) {
                // shouldInterceptRequest 在 WebView 内部 IO 线程执行（非主线程），必须同步返回结果，
                // 因此用 runBlocking 桥接协程下载；同时加超时与失败回退，避免长时间阻塞页面渲染。
                val ok = runBlocking {
                    withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS.milliseconds) {
                        download(cachePath, fileName) {
                            setUrl(url)
                            putHeader(request.requestHeaders)
                        }
                        true
                    }
                } ?: false
                if (!ok) {
                    // 超时或异常：删掉可能产生的空文件，让 WebView 走默认网络逻辑
                    if (file.exists() && file.length() == 0L) file.delete()
                    return null
                }
                lruCache.put(key, key)?.let { evicted -> File(evicted).delete() }
            }
            if (file.exists() && file.isFile && file.length() > 0L) {
                val mimeType = request.getMimeTypeFromUrl()
                WebResourceResponse(mimeType, null, file.inputStream()).apply {
                    responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "cacheResourceRequest failed: ${request.url}", e)
            null
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