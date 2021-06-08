package com.example.fragment.library.base.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.fragment.library.base.R
import com.example.fragment.library.base.component.view.SnailBar
import com.example.fragment.library.base.utils.InjectUtils.injectDarkModeJs
import com.example.fragment.library.base.utils.InjectUtils.injectVConsoleJs
import com.example.fragment.library.base.utils.InjectUtils.newDarkModeJs
import com.example.fragment.library.base.utils.InjectUtils.newVConsoleJs
import com.example.fragment.library.base.utils.UIModeUtils.isNightMode
import com.tencent.smtt.export.external.interfaces.IX5WebSettings
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.*
import java.io.ByteArrayInputStream
import java.util.regex.Pattern

class WebHelper private constructor(val parent: ViewGroup) {

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

    init {
        webView.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.white))
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            QbSdk.forceSysWebView()
            val view = webView.view
            if (view is android.webkit.WebView) {
                val forceDarkMode = if (parent.context.isNightMode()) {
                    WebSettingsCompat.FORCE_DARK_ON
                } else {
                    WebSettingsCompat.FORCE_DARK_OFF
                }
                WebSettingsCompat.setForceDark(view.settings, forceDarkMode)
            }
        } else {
            QbSdk.unForceSysWebView()
            webView.setDayOrNight(!parent.context.isNightMode())
        }
        val webSetting = webView.settings
        webSetting.allowFileAccess = true
        webSetting.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        webSetting.setSupportZoom(true)
        webSetting.builtInZoomControls = true
        webSetting.useWideViewPort = true
        webSetting.setSupportMultipleWindows(false)
        webSetting.setAppCacheEnabled(true)
        webSetting.domStorageEnabled = true
        webSetting.javaScriptEnabled = true
        webSetting.setGeolocationEnabled(true)
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE)
        webSetting.setAppCachePath(parent.context.getDir("appcache", 0).path)
        webSetting.databasePath = parent.context.getDir("databases", 0).path
        webSetting.setGeolocationDatabasePath(parent.context.getDir("geolocation", 0).path)
        webSetting.pluginState = WebSettings.PluginState.ON_DEMAND
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSetting.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
        webView.settingsExtension?.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.view.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                if (view != null && url != null) {
                    if (isImageUrl(url)) {
                        return webImageResponse(view.context, url)
                    }
                    if (url.startsWith("file:///android_asset/")) {
                        val index = url.lastIndexOf("/")
                        val filename = url.substring(index)
                        val suffixIndex = url.lastIndexOf(".")
                        val suffix = url.substring(suffixIndex + 1)
                        return assetsResponse(view.context, "$suffix$filename")
                    }
                }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (view != null && request != null) {
                    val url = request.url.toString()
                    if (isImageUrl(url)) {
                        return webImageResponse(view.context, url)
                    }
                    if (url.startsWith("file:///android_asset/")) {
                        val filenameIndex = url.lastIndexOf("/")
                        val filename = url.substring(filenameIndex)
                        val suffixIndex = url.lastIndexOf(".")
                        val suffix = url.substring(suffixIndex + 1)
                        return assetsResponse(view.context, "$suffix$filename")
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                onPageStartedListener?.onPageStarted(view, url, favicon)
                view?.apply {
                    evaluateJavascript(context.injectDarkModeJs()) {}
                    evaluateJavascript(context.newDarkModeJs()) {}
                    evaluateJavascript(context.injectVConsoleJs()) {}
                    evaluateJavascript(context.newVConsoleJs()) {}
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
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
                onProgressChangedListener?.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }
        parent.addView(
            webView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        parent.addView(
            progressBar,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    fun onDestroy() {
        parent.removeView(webView);
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

    private fun isImageUrl(url: String?): Boolean {
        return if (url == null || url.isBlank())
            false
        else
            Pattern.compile(".*?(jpeg|png|jpg|bmp)").matcher(url).matches()
    }

    private fun webImageResponse(context: Context, url: String): WebResourceResponse? {
        ImageLoader.with(context).load(url).submit()?.let { bytes ->
            val mimeType = getMimeTypeFromUrl(url, "image/png")
            val inputStream = ByteArrayInputStream(bytes)
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