package com.example.fragment.library.base.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.graphics.drawable.toBitmap
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import coil.executeBlocking
import coil.imageLoader
import coil.request.ImageRequest
import com.example.fragment.library.base.utils.InjectUtils.injectVConsoleJs
import com.example.fragment.library.base.utils.UIModeUtils.isNightMode
import com.example.fragment.library.base.view.SnailBar
import com.tencent.smtt.export.external.interfaces.IX5WebSettings
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.*
import com.tencent.smtt.sdk.WebView.HitTestResult.IMAGE_TYPE
import com.tencent.smtt.sdk.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

@SuppressLint("SetJavaScriptEnabled")
class WebHelper private constructor(private val parent: ViewGroup) {

    companion object {
        fun with(parent: ViewGroup): WebHelper {
            return WebHelper(parent)
        }
    }

    val webView = WebView(parent.context)
    val progressBar = SnailBar(parent.context)

    var onReceivedTitleListener: OnReceivedTitleListener? = null
    var onPageStartedListener: OnPageStartedListener? = null
    var onPageFinishedListener: OnPageFinishedListener? = null
    var onProgressChangedListener: OnProgressChangedListener? = null

    private var injectJs = false

    init {
        parent.addView(webView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        parent.addView(progressBar, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.view.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.settingsExtension?.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            QbSdk.forceSysWebView()
            webView.setBackgroundColor(Color.BLACK)
            val view = webView.view
            if (view is android.webkit.WebView) {
                val forceDarkMode = if (parent.context.isNightMode()) {
                    view.setBackgroundColor(Color.BLACK)
                    WebSettingsCompat.FORCE_DARK_ON
                } else {
                    view.setBackgroundColor(Color.WHITE)
                    WebSettingsCompat.FORCE_DARK_OFF
                }
                WebSettingsCompat.setForceDark(view.settings, forceDarkMode)
            }
        } else {
            QbSdk.unForceSysWebView()
            webView.setBackgroundColor(Color.WHITE)
            webView.setDayOrNight(!parent.context.isNightMode())
        }
        val webSetting = webView.settings
        webSetting.allowFileAccess = true
        webSetting.setAppCacheEnabled(true)
        webSetting.setAppCachePath(webView.context.getDir("appcache", MODE_PRIVATE).path)
        webSetting.domStorageEnabled = true
        webSetting.setGeolocationEnabled(true)
        webSetting.javaScriptEnabled = true
        webSetting.loadWithOverviewMode = true
        webSetting.useWideViewPort = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSetting.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return shouldOverrideUrlLoading(view, Uri.parse(url))
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return shouldOverrideUrlLoading(view, request?.url)
            }

            private fun shouldOverrideUrlLoading(view: WebView?, uri: Uri?): Boolean {
                if (view != null && uri != null && !("http" == uri.scheme || "https" == uri.scheme)) {
                    startActionView(view.context, uri)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                val webResourceResponse = shouldInterceptRequest(view, Uri.parse(url))
                return webResourceResponse ?: super.shouldInterceptRequest(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val webResourceResponse = shouldInterceptRequest(view, request?.url)
                return webResourceResponse ?: super.shouldInterceptRequest(view, request)
            }

            private fun shouldInterceptRequest(view: WebView?, uri: Uri?): WebResourceResponse? {
                if (view != null && uri != null) {
                    val url = uri.toString()
                    if (isImageUrl(url)) {
                        return webImageResponse(view.context, url)
                    } else if (url.startsWith("file:///android_asset/")) {
                        val filenameIndex = url.lastIndexOf("/")
                        val filename = url.substring(filenameIndex)
                        val suffixIndex = url.lastIndexOf(".")
                        val suffix = url.substring(suffixIndex + 1)
                        return assetsResponse(view.context, "$suffix$filename")
                    }
                }
                return null
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
        webView.setOnLongClickListener {
            val hitTestResult = webView.hitTestResult
            when (hitTestResult.type) {
                IMAGE_TYPE, SRC_IMAGE_ANCHOR_TYPE -> {
                    println(hitTestResult.extra)
                    true
                }
                else -> false
            }
        }
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    fun loadHtml(data: String) {
        loadData(data)
    }

    fun setWebDownloadListener(listener: WebDownloadListener) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            listener.onDownloadStart(url, userAgent, contentDisposition, mimetype, contentLength)
        }
    }

    fun startActionView(context: Context, uri: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private fun loadData(data: String, mimeType: String = "text/html", encoding: String = "utf-8") {
        webView.loadData(data, mimeType, encoding)
    }

    private fun isImageUrl(url: String?): Boolean {
        return if (url == null || url.isBlank())
            false
        else
            Pattern.compile(".*?(jpeg|png|jpg|bmp)").matcher(url).matches()
    }

    private fun webImageResponse(context: Context, url: String): WebResourceResponse? {
        val request = ImageRequest.Builder(context).data(url).build()
        context.imageLoader.executeBlocking(request).drawable?.let { drawable ->
            val bitmap = drawable.toBitmap()
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val mimeType = getMimeTypeFromUrl(url, "image/png")
            val inputStream = ByteArrayInputStream(baos.toByteArray())
            return WebResourceResponse(mimeType, "UTF-8", inputStream)
        }
        return null
    }

    private fun assetsResponse(context: Context, filename: String): WebResourceResponse? {
        try {
            val mimeType = getMimeTypeFromUrl(filename)
            val data = context.resources.assets.open(filename)
            return WebResourceResponse(mimeType, "UTF-8", data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getMimeTypeFromUrl(url: String, defType: String = ""): String {
        var mimeType = defType
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension.isNotBlank()) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return mimeType
    }

    interface WebDownloadListener {
        fun onDownloadStart(
            url: String,
            userAgent: String,
            contentDisposition: String,
            mimetype: String,
            contentLength: Long
        )
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