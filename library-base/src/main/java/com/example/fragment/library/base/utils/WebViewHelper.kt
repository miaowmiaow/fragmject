package com.example.fragment.library.base.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import coil.bitmap.BitmapPool
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.Options
import coil.executeBlocking
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.download
import com.example.fragment.library.base.utils.InjectUtils.injectVConsoleJs
import com.example.fragment.library.base.utils.UIModeUtils.isNightMode
import com.example.fragment.library.base.view.SnailBar
import com.tencent.smtt.export.external.interfaces.IX5WebSettings
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.*
import com.tencent.smtt.sdk.WebView.HitTestResult.IMAGE_TYPE
import com.tencent.smtt.sdk.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
import kotlinx.coroutines.runBlocking
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.util.*

class WebViewHelper private constructor(parent: ViewGroup) {

    companion object {
        fun with(parent: ViewGroup): WebViewHelper {
            return WebViewHelper(parent)
        }
    }

    private val webView = WebViewManager.obtain(parent.context)
    private val progressBar = SnailBar(parent.context)

    var onReceivedTitleListener: OnReceivedTitleListener? = null
    var onPageStartedListener: OnPageStartedListener? = null
    var onPageFinishedListener: OnPageFinishedListener? = null
    var onProgressChangedListener: OnProgressChangedListener? = null

    private var injectState = false
    private var injectVConsole = false
    private var originalUrl = "about:blank"

    init {
        parent.addView(webView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        parent.addView(progressBar, FrameLayout.LayoutParams(MATCH_PARENT, 5).apply {
            gravity = Gravity.BOTTOM
        })
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url
                if (view != null && uri != null && !("http" == uri.scheme || "https" == uri.scheme)) {
                    try {
                        view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url
                if (view != null && uri != null) {
                    val url = uri.toString()
                    val extension = getExtensionFromUrl(url)
                    when {
                        url.startsWith("file:///android_asset/") -> {
                            return assetsResponse(view.context, url)
                        }
                        isImage(extension) -> {
                            return imageResponse(view.context, url, extension)
                        }
                        isHtmlStyle(extension) -> {
                            return htmlStyleResponse(view.context, url, extension)
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                injectState = false
                progressBar.visibility = View.VISIBLE
                onPageStartedListener?.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectState = false
                progressBar.visibility = View.GONE
                onPageFinishedListener?.onPageFinished(view, url)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                onReceivedTitleListener?.onReceivedTitle(view, title)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (injectVConsole && !injectState && newProgress > 80) {
                    view?.apply {
                        evaluateJavascript(context.injectVConsoleJs()) {}
                    }
                    injectState = true
                }
                onProgressChangedListener?.onProgressChanged(view, newProgress)
            }
        }
        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                webView.context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            when (result.type) {
                IMAGE_TYPE, SRC_IMAGE_ANCHOR_TYPE -> {
                    println(result.extra)
                    true
                }
                else -> false
            }
        }
        val isAppDarkMode = webView.context.isNightMode()
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            QbSdk.forceSysWebView()
            val view = webView.view
            if (view is android.webkit.WebView) {
                val forceDarkMode = if (isAppDarkMode) {
                    WebSettingsCompat.FORCE_DARK_ON
                } else {
                    WebSettingsCompat.FORCE_DARK_OFF
                }
                WebSettingsCompat.setForceDark(view.settings, forceDarkMode)
            }
        } else {
            QbSdk.unForceSysWebView()
            webView.setDayOrNight(!isAppDarkMode)
        }
    }

    fun injectVConsole(inject: Boolean): WebViewHelper {
        injectVConsole = inject
        return this
    }

    fun canGoBack(): Boolean {
        val canBack = webView.canGoBack()
        if (canBack) webView.goBack()
        val backForwardList = webView.copyBackForwardList()
        val currentIndex = backForwardList.currentIndex
        if (currentIndex == 0) {
            val currentUrl = backForwardList.currentItem.url
            val currentHost = Uri.parse(currentUrl).host
            //栈底不是链接则直接返回
            if (currentHost.isNullOrBlank()) return false
            //栈底链接不是原始链接则直接返回
            if (originalUrl != currentUrl) return false
        }
        return canBack
    }

    fun canGoForward(): Boolean {
        val canForward = webView.canGoForward()
        if (canForward) webView.goForward()
        return canForward
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
        originalUrl = url
    }

    fun reload() {
        webView.reload()
    }

    fun onResume() {
        webView.onResume()
    }

    fun onPause() {
        webView.onPause()
    }

    fun onDestroyView() {
        WebViewManager.recycle(webView)
    }

    private fun isImage(extension: String): Boolean {
        return extension == "ico"
                || extension == "gif"
                || extension == "jpeg"
                || extension == "jpg"
                || extension == "png"
                || extension == "svg"
                || extension == "webp"
    }

    private fun isHtmlStyle(extension: String): Boolean {
        return extension == "css" || extension == "js"
    }

    private fun assetsResponse(context: Context, url: String): WebResourceResponse? {
        try {
            val webResourceResponse = WebResourceResponse()
            val filenameIndex = url.lastIndexOf("/") + 1
            val filename = url.substring(filenameIndex)
            val suffixIndex = url.lastIndexOf(".")
            val suffix = url.substring(suffixIndex + 1)
            webResourceResponse.mimeType = getMimeTypeFromExtension(url)
            webResourceResponse.encoding = "UTF-8"
            webResourceResponse.data = context.resources.assets.open("$suffix/$filename")
            return webResourceResponse
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 接管图片加载
     */
    private fun imageResponse(
        context: Context,
        url: String,
        extension: String
    ): WebResourceResponse? {
        try {
            val webResourceResponse = WebResourceResponse()
            context.imageLoader.executeBlocking(
                ImageRequest.Builder(context).data(url).decoder(object : Decoder {
                    override suspend fun decode(
                        pool: BitmapPool,
                        source: BufferedSource,
                        size: Size,
                        options: Options
                    ): DecodeResult {
                        val byteArray = source.use { it.peek().inputStream().readBytes() }
                        val responseHeaders = mapOf("access-control-allow-origin" to "*")
                        webResourceResponse.mimeType = getMimeTypeFromExtension(extension)
                        webResourceResponse.encoding = "UTF-8"
                        webResourceResponse.data = byteArray.inputStream()
                        webResourceResponse.responseHeaders = responseHeaders
                        return DecodeResult(ColorDrawable(), false)
                    }

                    override fun handles(source: BufferedSource, mimeType: String?) = false

                }).build()
            )
            return webResourceResponse
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 接管css、js加载
     */
    private fun htmlStyleResponse(
        context: Context,
        url: String,
        extension: String
    ): WebResourceResponse? {
        try {
            val cachePath = CacheUtils.getCacheDirPath(context, "html")
            val filePathName = cachePath + File.separator + url.encodeUtf8().md5().hex()
            val file = File(filePathName)
            if (!file.exists() || !file.isFile) {
                runBlocking {
                    download(HttpRequest(url), filePathName)
                }
            }
            if (file.exists() && file.isFile) {
                val webResourceResponse = WebResourceResponse()
                webResourceResponse.mimeType = getMimeTypeFromExtension(extension)
                webResourceResponse.encoding = "UTF-8"
                webResourceResponse.data = file.inputStream()
                return webResourceResponse
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getExtensionFromUrl(url: String): String {
        try {
            if (url.isNotBlank() && url != "null") {
                return MimeTypeMap.getFileExtensionFromUrl(url)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun getMimeTypeFromExtension(extension: String): String {
        try {
            if (extension.isNotBlank() && extension != "null") {
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "*/*"
    }


    interface OnReceivedTitleListener {
        fun onReceivedTitle(view: WebView?, title: String?)
    }

    interface OnPageStartedListener {
        fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?)
    }

    interface OnPageFinishedListener {
        fun onPageFinished(view: WebView?, url: String?)
    }

    interface OnProgressChangedListener {
        fun onProgressChanged(view: WebView?, newProgress: Int)
    }

}

@SuppressLint("SetJavaScriptEnabled")
class WebViewManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: WebViewManager? = null

        private fun instance() = INSTANCE ?: synchronized(this) {
            INSTANCE ?: WebViewManager().also { INSTANCE = it }
        }

        fun obtain(context: Context): WebView {
            return instance().obtain(context)
        }

        fun recycle(webView: WebView) {
            instance().recycle(webView)
        }

        fun destroy() {
            instance().destroy()
        }
    }

    private val webViewCache: MutableList<WebView> = ArrayList(1)

    private fun create(context: Context): WebView {
        val webView = WebView(context)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.view.setBackgroundColor(Color.TRANSPARENT)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.view.overScrollMode = WebView.OVER_SCROLL_NEVER
        val webSetting = webView.settings
        webSetting.allowFileAccess = true
        webSetting.setAppCacheEnabled(true)
        webSetting.cacheMode = WebSettings.LOAD_DEFAULT
        webSetting.domStorageEnabled = true
        webSetting.setGeolocationEnabled(true)
        webSetting.javaScriptEnabled = true
        webSetting.loadWithOverviewMode = true
        webSetting.useWideViewPort = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSetting.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
        webView.settingsExtension?.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY)
        return webView
    }

    fun obtain(context: Context): WebView {
        if (webViewCache.isEmpty()) {
            webViewCache.add(create(context))
        }
        val webView = webViewCache.removeAt(0)
        webView.clearHistory()
        webView.resumeTimers()
        return webView
    }

    fun recycle(webView: WebView) {
        try {
            webView.stopLoading()
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            webView.clearHistory()
            webView.pauseTimers()
            webView.webChromeClient = null
            webView.webViewClient = null
            val parent = webView.parent
            if (parent != null) {
                (parent as ViewGroup).removeView(webView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (!webViewCache.contains(webView)) {
                webViewCache.add(webView)
            }
        }
    }

    fun destroy() {
        try {
            webViewCache.forEach {
                it.removeAllViews()
                it.destroy()
                webViewCache.remove(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}