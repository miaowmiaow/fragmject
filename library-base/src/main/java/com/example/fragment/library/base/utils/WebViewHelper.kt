package com.example.fragment.library.base.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
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

    private val webView = WebViewCache.obtain(parent.context)
    private val progressBar = SnailBar(parent.context)

    var onReceivedTitleListener: OnReceivedTitleListener? = null
    var onPageStartedListener: OnPageStartedListener? = null
    var onPageFinishedListener: OnPageFinishedListener? = null
    var onProgressChangedListener: OnProgressChangedListener? = null

    private var injectState = false
    private var injectVConsole = false

    init {
        parent.addView(webView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        parent.addView(progressBar, ViewGroup.LayoutParams(MATCH_PARENT, 10))
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
                val accept = request?.requestHeaders?.get("Accept")
                if (view != null && uri != null) {
                    val url = uri.toString()
                    when {
                        url.startsWith("file:///android_asset/") -> {
                            return assetsResponse(view.context, url)
                        }
                        isImage(url, accept) -> {
                            return imageResponse(view.context, url, accept)
                        }
                        isHtmlStyle(url, accept) -> {
                            return htmlStyleResponse(view.context, url, accept)
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
        val can = webView.canGoBack()
        if (can) {
            webView.goBack()
        }
        return can
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    fun onResume() {
        webView.resumeTimers()
        webView.onResume()
    }

    fun onPause() {
        webView.pauseTimers()
        webView.onPause()
    }

    fun onDestroy() {
        WebViewCache.destroy(webView)
    }

    private fun isImage(url: String, accept: String?): Boolean {
        if (url.endsWith(".gif")
            || url.endsWith(".jpeg")
            || url.endsWith(".jpg")
            || url.endsWith(".png")
            || url.endsWith(".svg")
            || url.endsWith(".webp")
        ) {
            return true
        }
        if (accept.isNullOrBlank()) {
            return false
        }
        val contentType = accept.split(",")[0]
        return contentType.contains("image/")
    }

    private fun isHtmlStyle(url: String, accept: String?): Boolean {
        if (url.endsWith(".css") || url.endsWith(".js") || url.endsWith(".json")) {
            return true
        }
        if (accept.isNullOrBlank()) {
            return false
        }
        val contentType = accept.split(",")[0]
        return contentType.contains("css") || contentType.contains("javascript") || contentType.contains(
            "json"
        )
    }

    private fun assetsResponse(context: Context, url: String): WebResourceResponse? {
        try {
            val webResourceResponse = WebResourceResponse()
            val filenameIndex = url.lastIndexOf("/") + 1
            val filename = url.substring(filenameIndex)
            val suffixIndex = url.lastIndexOf(".")
            val suffix = url.substring(suffixIndex + 1)
            webResourceResponse.mimeType = getMimeTypeFromUrl(url, "*/*")
            webResourceResponse.encoding = "UTF-8"
            webResourceResponse.data = context.resources.assets.open("$suffix/$filename")
            return webResourceResponse
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun imageResponse(
        context: Context,
        url: String,
        accept: String?
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
                        webResourceResponse.mimeType = getMimeTypeFromUrl(url, accept)
                        webResourceResponse.encoding = "UTF-8"
                        webResourceResponse.data = byteArray.inputStream()
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

    private fun htmlStyleResponse(
        context: Context,
        url: String,
        accept: String?
    ): WebResourceResponse? {
        try {
            val cachePath = CacheUtils.getCacheDirPath(context, "html")
            val filePathName = cachePath + File.separator + url.encodeUtf8().md5().hex()
            val file = File(filePathName)
            if (!file.exists() || !file.isFile) {
                val response = runBlocking {
                    download(HttpRequest(url), filePathName)
                }
                if (response.errorCode == "0") {
                    val webResourceResponse = WebResourceResponse()
                    webResourceResponse.mimeType = getMimeTypeFromUrl(url, accept)
                    webResourceResponse.encoding = "UTF-8"
                    webResourceResponse.data = file.inputStream()
                    return webResourceResponse
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getMimeTypeFromUrl(url: String, accept: String?): String {
        var mimeType = "*/*"
        try {
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            if (extension.isNotBlank()) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
            if (mimeType == "*/*" && !accept.isNullOrBlank()) {
                mimeType = accept.split(",")[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mimeType
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

class WebViewCache private constructor() {

    companion object {
        @Volatile
        private var instance: WebViewCache? = null

        private fun instance() = instance ?: synchronized(this) {
            instance ?: WebViewCache().also { instance = it }
        }

        fun obtain(context: Context): WebView {
            return instance().obtain(context)
        }

        fun destroy(webView: WebView) {
            instance().destroy(webView)
        }
    }

    private val webCache: MutableList<WebView> = ArrayList(1)

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
        if (webCache.isEmpty()) {
            webCache.add(create(context))
        }
        val webView = webCache.removeAt(0)
        if (webCache.isEmpty()) {
            webCache.add(create(context))
        }
        return webView
    }

    fun destroy(webView: WebView) {
        try {
            val parent = webView.parent
            if (parent != null) {
                (parent as ViewGroup).removeView(webView)
            }
            webView.stopLoading()
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            webView.webChromeClient = null
            webView.webViewClient = null
            webView.removeAllViews()
            webView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            webCache.remove(webView)
        }
    }

}