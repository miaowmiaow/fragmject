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

class WebHelper private constructor(private val parent: ViewGroup) {

    companion object {
        fun with(parent: ViewGroup): WebHelper {
            return WebHelper(parent)
        }
    }

    private val webView = WebView(parent.context)
    private val progressBar = SnailBar(parent.context)

    var onReceivedTitleListener: OnReceivedTitleListener? = null
    var onPageStartedListener: OnPageStartedListener? = null
    var onPageFinishedListener: OnPageFinishedListener? = null
    var onProgressChangedListener: OnProgressChangedListener? = null

    private var injectJs = false

    init {
        parent.addView(webView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        parent.addView(progressBar, ViewGroup.LayoutParams(MATCH_PARENT, 15))
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
                    if (isImage(accept)) {
                        return imageResponse(view.context, url)
                    } else if (url.endsWith(".js") || isHtml(accept)) {
                        return htmlResponse(view.context, url)
                    } else if (url.startsWith("file:///android_asset/")) {
                        return assetsResponse(view.context, url)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                injectJs = false
                progressBar.visibility = View.VISIBLE
                onPageStartedListener?.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectJs = false
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
                if (!injectJs && newProgress > 80) {
                    view?.apply {
                        evaluateJavascript(context.injectVConsoleJs()) {}
                    }
                    injectJs = true
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

    fun getWebView(): WebView {
        return webView
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
        parent.removeView(webView)
        try {
            webView.removeAllViews()
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = null
            webView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isImage(accept: String?): Boolean {
        if (accept.isNullOrBlank()) {
            return false
        }
        return accept.contains("image/*")
    }

    private fun isHtml(accept: String?): Boolean {
        if (accept.isNullOrBlank()) {
            return false
        }
        return accept.contains("text/javascript") || accept.contains("text/css")
    }

    private fun imageResponse(context: Context, url: String): WebResourceResponse {
        val webResourceResponse = WebResourceResponse()
        val request = ImageRequest.Builder(context).data(url).decoder(object : Decoder {
            override suspend fun decode(
                pool: BitmapPool,
                source: BufferedSource,
                size: Size,
                options: Options
            ): DecodeResult {
                val byteArray = source.use { it.peek().inputStream().readBytes() }
                webResourceResponse.mimeType = getMimeTypeFromUrl(url, "image/webp")
                webResourceResponse.encoding = "UTF-8"
                webResourceResponse.data = byteArray.inputStream()
                return DecodeResult(ColorDrawable(), false)
            }

            override fun handles(source: BufferedSource, mimeType: String?) = false

        }).build()
        context.imageLoader.executeBlocking(request)
        return webResourceResponse
    }

    private fun htmlResponse(context: Context, url: String): WebResourceResponse {
        val webResourceResponse = WebResourceResponse()
        val path = CacheUtils.getCacheDirectory(context, "html").absolutePath
        val filePathName = path + File.separator + url.encodeUtf8().md5().hex()
        val file = File(filePathName)
        runBlocking {
            if (!file.exists() || !file.isFile) {
                download(HttpRequest(url), filePathName)
            }
        }
        webResourceResponse.mimeType = getMimeTypeFromUrl(url)
        webResourceResponse.encoding = "UTF-8"
        webResourceResponse.data = file.inputStream()
        return webResourceResponse
    }


    private fun assetsResponse(context: Context, url: String): WebResourceResponse {
        val webResourceResponse = WebResourceResponse()
        try {
            val filenameIndex = url.lastIndexOf("/")
            val filename = url.substring(filenameIndex)
            val suffixIndex = url.lastIndexOf(".")
            val suffix = url.substring(suffixIndex + 1)
            webResourceResponse.mimeType = getMimeTypeFromUrl(suffix + filename)
            webResourceResponse.encoding = "UTF-8"
            webResourceResponse.data = context.resources.assets.open(suffix + filename)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return webResourceResponse
    }

    private fun getMimeTypeFromUrl(url: String, defType: String = "*/*"): String {
        var mimeType = defType
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension.isNotBlank()) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
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