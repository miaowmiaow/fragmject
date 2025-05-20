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
import com.example.miaow.base.http.download
import com.example.miaow.base.utils.CacheUtils
import com.example.miaow.base.utils.LRUCache
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

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
    private val backStack: ArrayDeque<String> = ArrayDeque()
    private val lruCache: LRUCache<String, String> = LRUCache(5000)
    private val acceptImage = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

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

    private fun prepare(context: Context) {
        Looper.myQueue().addIdleHandler {
            val cachePath = CacheUtils.getDirPath(context, "web_cache")
            File(cachePath).takeIf { it.isDirectory }?.listFiles()?.sortedWith(compareByDescending {
                //文件创建时间越久说明使用频率越高
                //通过时间倒序排序防止高频文件初始化时位于队首易被淘汰
                val attrs = Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                attrs.creationTime().toMillis()
            })?.forEach {
                val absolutePath = it.absolutePath
                //put会返回被被淘汰的元素
                lruCache.put(absolutePath, absolutePath)?.let { path ->
                    //删除被淘汰的文件
                    File(path).delete()
                }
            }
            false
        }
    }

    private fun obtain(context: Context, url: String): WebView {
        val webView = webViewMap.getOrPut(url) {
            backStack.add(url)
            create(context)
        }
        if (webView.parent != null) {
            (webView.parent as ViewGroup).removeView(webView)
        }
        if (webViewMap.size > 50) {
            try {
                webViewMap.remove(backStack.removeFirst())?.let {
                    it.removeParentView()
                    it.removeAllViews()
                    it.destroy()
                }
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
            }
        }
        return webView
    }

    private fun recycle(webView: WebView) {
        try {
            webView.removeParentView()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    private fun destroy() {
        try {
            webViewMap.destroyWebView()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    private fun MutableMap<String, WebView>.destroyWebView() {
        values.toList().forEach {
            it.removeParentView()
            it.removeAllViews()
            it.destroy()
        }
        clear()
    }

    private fun WebView.removeParentView(): WebView {
        if (parent != null) {
            (parent as ViewGroup).removeView(this)
        }
        val contextWrapper = context as MutableContextWrapper
        contextWrapper.baseContext = context.applicationContext
        return this
    }

    fun isAssetsResource(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        return url.startsWith("file:///android_asset/")
    }

    fun isCacheResource(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        //忽略掉百度统计
        if (url.contains("hm.baidu.com/hm.gif")) return false
        val extension = request.getExtensionFromUrl()
        if (extension == "text/html") return true
        if (extension.isBlank()) {
            val accept = request.requestHeaders["Accept"] ?: return false
            if (accept == acceptImage && request.method.equals("GET", true)) {
                return true
            }
        }
        return extension in listOf(
            "ico",
            "bmp",
            "gif",
            "jpeg",
            "jpg",
            "png",
            "svg",
            "webp",
            "css",
            "js",
            "json",
            "eot",
            "otf",
            "ttf",
            "woff"
        )
    }

    fun assetsResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        try {
            val url = request.url.toString()
            val filename = url.substringAfterLast("/")
            val suffix = url.substringAfterLast(".")
            val mimeType = request.getMimeTypeFromUrl()
            val encoding = context.assets.open(suffix + File.separator + filename)
            return WebResourceResponse(mimeType, null, encoding).apply {
                responseHeaders = mapOf("access-control-allow-origin" to "*")
            }
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
            val file = File(key)
            if (!file.exists() || !file.isFile) {
                runBlocking {
                    download(cachePath, fileName) {
                        setUrl(url)
                        putHeader(request.requestHeaders)
                    }
                    lruCache.put(key, key)?.let { path ->
                        File(path).delete()
                    }
                }
            }
            if (file.exists() && file.isFile) {
                val mimeType = request.getMimeTypeFromUrl()
                return WebResourceResponse(mimeType, null, file.inputStream()).apply {
                    responseHeaders = mapOf("access-control-allow-origin" to "*")
                }
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return null
    }

    private fun WebResourceRequest.getExtensionFromUrl(): String {
        return try {
            MimeTypeMap.getFileExtensionFromUrl(url.toString())
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            "*/*"
        }
    }

    private fun WebResourceRequest.getMimeTypeFromUrl(): String {
        return try {
            when (val extension = getExtensionFromUrl()) {
                "", "null", "*/*" -> "*/*"
                "json" -> "application/json"
                "text/html" -> extension
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            "*/*"
        }
    }
}