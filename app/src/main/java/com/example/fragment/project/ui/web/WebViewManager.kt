package com.example.fragment.project.ui.web

import android.annotation.SuppressLint
import android.content.Context
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
import com.example.fragment.project.utils.LRUCache
import com.example.miaow.base.http.download
import com.example.miaow.base.utils.CacheUtils
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.lang.ref.WeakReference

@SuppressLint("SetJavaScriptEnabled")
class WebViewManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: WebViewManager? = null

        private fun getInstance() = INSTANCE ?: synchronized(WebViewManager::class.java) {
            INSTANCE ?: WebViewManager().also { INSTANCE = it }
        }

        fun prepare(context: Context) {
            getInstance().prepare(context)
        }

        fun destroy() {
            getInstance().destroy()
        }

        fun obtain(context: Context, url: String): WebView {
            return getInstance().obtain(context, url)
        }

        fun back(webView: WebView): String {
            return getInstance().back(webView)
        }

        fun forward(webView: WebView): String {
            return getInstance().forward(webView)
        }

        fun recycle(webView: WebView) {
            getInstance().recycle(webView)
        }

        fun isAssetsResource(request: WebResourceRequest): Boolean {
            return getInstance().isAssetsResource(request)
        }

        fun isCacheResource(request: WebResourceRequest): Boolean {
            return getInstance().isCacheResource(request)
        }

        fun assetsResourceRequest(
            context: Context,
            request: WebResourceRequest
        ): WebResourceResponse? {
            return getInstance().assetsResourceRequest(context, request)
        }

        fun cacheResourceRequest(
            context: Context,
            request: WebResourceRequest
        ): WebResourceResponse? {
            return getInstance().cacheResourceRequest(context, request)
        }
    }

    private val webViewMap = mutableMapOf<String, WebView>()
    private val webViewQueue: ArrayDeque<WebView> = ArrayDeque()
    private val backStack: ArrayDeque<String> = ArrayDeque()
    private val forwardStack: ArrayDeque<String> = ArrayDeque()
    private var lastBackWebView: WeakReference<WebView?> = WeakReference(null)
    private val lruCache: LRUCache<String, File> = LRUCache(500)

    private fun create(context: Context): WebView {
        val webView = WebView(context)
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

    private fun getWebView(context: Context): WebView {
        val webView = if (webViewQueue.isEmpty()) {
            create(MutableContextWrapper(context))
        } else {
            webViewQueue.removeFirst()
        }
        addQueue(MutableContextWrapper(context.applicationContext))
        return webView
    }

    private fun addQueue(context: Context) {
        if (webViewQueue.isEmpty()) {
            webViewQueue.add(create(MutableContextWrapper(context.applicationContext)))
        }
    }

    private fun prepare(context: Context) {
        Looper.myQueue().addIdleHandler {
            addQueue(context)
            val cachePath = CacheUtils.getDirPath(context, "web_cache")
            File(cachePath).takeIf { it.isDirectory }?.listFiles()?.forEach {
                lruCache.put(it.absolutePath, it)
            }
            false
        }
    }

    private fun destroy() {
        try {
            backStack.clear()
            forwardStack.clear()
            lastBackWebView.clear()
            webViewMap.destroyWebView()
            webViewQueue.destroyWebView()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    private fun obtain(context: Context, url: String): WebView {
        val webView = webViewMap.getOrPut(url) {
            getWebView(MutableContextWrapper(context))
        }
        if (webView.parent != null) {
            (webView.parent as ViewGroup).removeView(webView)
        }
        val contextWrapper = webView.context as MutableContextWrapper
        contextWrapper.baseContext = context
        return webView
    }

    private fun back(webView: WebView): String {
        return try {
            val backLastUrl = backStack.removeLast()//通过NoSuchElementException判断是否处在第一页
            forwardStack.add(webView.originalUrl.toString())
            backLastUrl
        } catch (e: Exception) {
            lastBackWebView = WeakReference(webView)
            ""
        }
    }

    private fun forward(webView: WebView): String {
        return try {
            val forwardLastUrl = forwardStack.removeLast()
            backStack.add(webView.originalUrl.toString())
            forwardLastUrl
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ""
        }
    }

    private fun recycle(webView: WebView) {
        try {
            webView.removeParentView()
            val originalUrl = webView.originalUrl.toString()
            if (lastBackWebView.get() != webView) {
                if (!forwardStack.contains(originalUrl)) {
                    backStack.add(originalUrl)
                }
            } else {
                destroy()
                //重新缓存一个webView
                addQueue(webView.context)
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    private fun WebView.removeParentView(): WebView {
        if (parent != null) {
            (parent as ViewGroup).removeView(this)
        }
        val contextWrapper = context as MutableContextWrapper
        contextWrapper.baseContext = context.applicationContext
        return this
    }

    private fun MutableList<WebView>.destroyWebView() {
        forEach {
            it.removeParentView()
            it.removeAllViews()
            it.destroy()
        }
        clear()
    }

    private fun MutableMap<String, WebView>.destroyWebView() {
        values.toList().forEach {
            it.removeParentView()
            it.removeAllViews()
            it.destroy()
        }
        clear()
    }

    fun isAssetsResource(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        return url.startsWith("file:///android_asset/")
    }

    fun isCacheResource(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        val extension = url.getExtensionFromUrl()
        if (extension.isBlank()) {
            val accept = request.requestHeaders["Accept"] ?: return false
            if (accept.contains("image/avif,image/webp,image/apng,image/svg xml,image/*,*/*;q=0.8")) {
                return true
            }
        }
        return extension == "ico" || extension == "bmp" || extension == "gif"
                || extension == "jpeg" || extension == "jpg" || extension == "png"
                || extension == "svg" || extension == "webp" || extension == "css"
                || extension == "js" || extension == "json" || extension == "eot"
                || extension == "otf" || extension == "ttf" || extension == "woff"
    }

    fun assetsResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        try {
            val url = request.url.toString()
            val filename = url.substringAfterLast("/")
            val suffix = url.substringAfterLast(".")
            val webResourceResponse = WebResourceResponse(
                url.getMimeTypeFromUrl(),
                null,
                context.assets.open(suffix + File.separator + filename)
            )
            webResourceResponse.responseHeaders = mapOf("access-control-allow-origin" to "*")
            return webResourceResponse
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return null
    }

    fun cacheResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        try {
            val url = request.url.toString()
            val cachePath = CacheUtils.getDirPath(context, "web_cache")
            val fileName = url.encodeUtf8().md5().hex()
            val key = cachePath + File.separator + fileName
            var file = lruCache.get(key)
            if (file == null || !file.exists() || !file.isFile) {
                runBlocking {
                    download(cachePath, fileName) {
                        setUrl(url)
                        putHeader(request.requestHeaders)
                    }
                }
                file = File(key)
                lruCache.put(key, file)?.delete()
            }
            if (file.exists() && file.isFile) {
                val webResourceResponse = WebResourceResponse(
                    url.getMimeTypeFromUrl(),
                    null,
                    file.inputStream()
                )
                webResourceResponse.responseHeaders = mapOf("access-control-allow-origin" to "*")
                return webResourceResponse
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return null
    }

    private fun String.getExtensionFromUrl(): String {
        try {
            if (isNotBlank() && this != "null") {
                return MimeTypeMap.getFileExtensionFromUrl(this)
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return ""
    }

    private fun String.getMimeTypeFromUrl(): String {
        try {
            val extension = this.getExtensionFromUrl()
            if (extension.isNotBlank() && extension != "null") {
                if (extension == "json") {
                    return "application/json"
                }
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return "*/*"
    }
}