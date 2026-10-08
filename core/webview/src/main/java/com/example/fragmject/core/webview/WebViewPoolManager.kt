package com.example.fragmject.core.webview

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.fragmject.core.android.platform.app.AppCoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebView 管理器：维护 WebView 池的完整生命周期。
 * - 维护一个空闲 WebView 实例做热身复用，使用 [MutableContextWrapper] 在 Activity 之间安全切换 baseContext，
 *   不再以 url 为 key 持有多个实例，避免内存堆积与回调闭包泄漏。
 * - 维护 keep-alive 池，实现「详情页返回时不重新加载、不丢失操作」。
 *
 * 缓存与加载优化职责已拆至 [WebResourceCacheManager]。
 */
@SuppressLint("SetJavaScriptEnabled")
@Singleton
class WebViewPoolManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appScope: AppCoroutineScope,
) : WebViewPool {

    companion object {
        private const val TAG = "WebViewPoolManager"

        /** 预热兜底延迟：主线程长时间无空闲时强制执行，避免预热永不发生。 */
        private const val WARMUP_FALLBACK_DELAY_MS = 4_000L
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
        // 第三方 Cookie 开关需要 WebView 实例，故留在池内直接设置，不进入 CookieStore 领域契约
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        return webView
    }

    /**
     * 应用启动后调用：
     * 用 ApplicationContext 预创建一个空闲 WebView，下次 obtain 直接复用，省掉首屏 WebView 初始化耗时。
     */
    override fun prepare(context: Context) {
        val appCtx = context.applicationContext
        val handler = Handler(appCtx.mainLooper)
        val warmup = Runnable { warmupSpareWebView(appCtx) }
        // 用 IdleHandler 把预热放到主线程**真正空闲**时：new WebView 会触发 provider/so
        // 加载与渲染进程预热，典型 50–300ms 主线程停顿；固定 postDelayed(500) 无法保证
        // 那一刻主线程是空闲的，往往正好落在首屏加载/骨架屏期间造成掉帧。
        // 延迟任务仅作兜底：主线程长时间不空闲时强制执行（重复执行由 spareWebView 判空吸收）。
        handler.post {
            Looper.myQueue().addIdleHandler {
                warmup.run()
                false // 只执行一次
            }
            handler.postDelayed(warmup, WARMUP_FALLBACK_DELAY_MS)
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
    override fun obtain(context: Context, url: String): WebView {
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
                appScope.launch(Dispatchers.Main) {
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

    override fun setDebuggingEnabled(enabled: Boolean) {
        WebView.setWebContentsDebuggingEnabled(enabled)
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
}