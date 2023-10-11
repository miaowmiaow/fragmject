package com.example.fragment.project.ui.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.miaow.base.dialog.StandardDialog
import com.example.miaow.base.http.download
import com.example.miaow.base.utils.CacheUtils
import com.example.miaow.base.utils.saveImagesToAlbum
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

        fun obtain(context: Context): WebView {
            return getInstance().obtain(context)
        }

        fun back(webView: WebView): Boolean {
            return getInstance().back(webView)
        }

        fun forward(webView: WebView): Boolean {
            return getInstance().forward(webView)
        }

        fun recycle(webView: WebView, canRecycle: Boolean) {
            getInstance().recycle(webView, canRecycle)
        }

        fun destroy() {
            getInstance().destroy()
        }
    }

    private val webViewCache: MutableList<WebView> = ArrayList(1)
    private val backStack: ArrayDeque<WebView> = ArrayDeque()
    private val forwardStack: ArrayDeque<WebView> = ArrayDeque()
    private var excludeWebView: WeakReference<WebView?> = WeakReference(null)

    private fun create(context: Context): WebView {
        val webView = WebView(context)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        val webSettings = webView.settings
        webSettings.allowFileAccess = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.domStorageEnabled = true
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.setSupportZoom(true)
        webSettings.displayZoomControls = false
        webSettings.useWideViewPort = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        return webView
    }

    fun prepare(context: Context) {
        if (webViewCache.isEmpty()) {
            Looper.myQueue().addIdleHandler {
                webViewCache.add(create(MutableContextWrapper(context.applicationContext)))
                false
            }
        }
    }

    fun obtain(context: Context): WebView {
        if (webViewCache.isEmpty()) {
            webViewCache.add(create(MutableContextWrapper(context)))
        }
        val webView = webViewCache.removeFirst()
        val contextWrapper = webView.context as MutableContextWrapper
        contextWrapper.baseContext = context
        return webView
    }

    fun back(webView: WebView): Boolean {
        return try {
            webViewCache.add(0, backStack.removeLast())
            forwardStack.add(webView)
            true
        } catch (e: Exception) {
            excludeWebView = WeakReference(webView)
            false
        }
    }

    fun forward(webView: WebView): Boolean {
        return try {
            webViewCache.add(0, forwardStack.removeLast())
            backStack.add(webView)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun recycle(webView: WebView, canRecycle: Boolean) {
        try {
            removeParentView(webView)
            if (canRecycle) {
                if (excludeWebView.get() != webView) {
                    if (!backStack.contains(webView) && !forwardStack.contains(webView)) {
                        backStack.addLast(webView)
                    }
                } else {
                    excludeWebView.clear()
                    backStack.clear()
                    forwardStack.clear()
                    webView.stopLoading()
                    webView.clearHistory()
                    webView.loadDataWithBaseURL("about:blank", "", "text/html", "utf-8", null)
                    webViewCache.add(0, webView)
                }
            } else {
                webViewCache.add(0, webView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            excludeWebView.get()?.let { removeParentView(it) }
            excludeWebView.clear()
            destroyWebView(backStack)
            destroyWebView(forwardStack)
            destroyWebView(webViewCache)
            backStack.clear()
            forwardStack.clear()
            webViewCache.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeParentView(webView: WebView) {
        val parent = webView.parent
        if (parent != null) {
            (parent as ViewGroup).removeView(webView)
        }
        val contextWrapper = webView.context as MutableContextWrapper
        contextWrapper.baseContext = webView.context.applicationContext
    }

    private fun destroyWebView(list: List<WebView>) {
        list.forEach {
            removeParentView(it)
            it.removeAllViews()
            it.destroy()
        }
    }

}

fun WebView.setDownloadListener() {
    setDownloadListener { url, _, _, _, _ ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun WebView.setOnLongClickListener() {
    setOnLongClickListener {
        val result = hitTestResult
        when (result.type) {
            WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                result.extra?.let { extra ->
                    val contextWrapper = context as MutableContextWrapper
                    contextWrapper.baseContext.saveImageBase64Dialog(extra)
                }
                true
            }

            else -> false
        }
    }
}

private fun Context.saveImageBase64Dialog(data: String) {
    StandardDialog.newInstance()
        .setContent("你希望保存该图片吗?")
        .setOnDialogClickListener(object :
            StandardDialog.OnDialogClickListener {
            override fun onConfirm(dialog: StandardDialog) {
                if (URLUtil.isValidUrl(data)) {
                    saveImagesToAlbum(data) { _, _ -> }
                } else {
                    var str = data
                    if (str.contains(",")) {
                        str = str.split(",")[1]
                    }
                    val array = Base64.decode(str, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(array, 0, array.size)
                    saveImagesToAlbum(bitmap) { _, _ -> }
                }
            }

            override fun onCancel(dialog: StandardDialog) {
            }

        })
        .show(this)
}

fun WebResourceRequest.isAssetsResource(): Boolean {
    val url = url.toString()
    return url.startsWith("file:///android_asset/")
}

fun WebResourceRequest.isCacheResource(): Boolean {
    val url = url.toString()
    val extension = url.getExtensionFromUrl()
    return extension == "ico" || extension == "bmp" || extension == "gif"
            || extension == "jpeg" || extension == "jpg" || extension == "png"
            || extension == "svg" || extension == "webp" || extension == "css"
            || extension == "js" || extension == "json" || extension == "eot"
            || extension == "otf" || extension == "ttf" || extension == "woff"
}

fun WebResourceRequest.assetsResourceRequest(context: Context): WebResourceResponse? {
    try {
        val url = url.toString()
        val filenameIndex = url.lastIndexOf("/") + 1
        val filename = url.substring(filenameIndex)
        val suffixIndex = url.lastIndexOf(".")
        val suffix = url.substring(suffixIndex + 1)
        val webResourceResponse = WebResourceResponse(
            url.getMimeTypeFromUrl(),
            "UTF-8",
            context.assets.open("$suffix/$filename")
        )
        webResourceResponse.responseHeaders = mapOf("access-control-allow-origin" to "*")
        return webResourceResponse
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun WebResourceRequest.cacheResourceRequest(context: Context): WebResourceResponse? {
    try {
        val url = url.toString()
        val savePath = CacheUtils.getDirPath(context, "web_cache")
        val fileName = url.encodeUtf8().md5().hex()
        val file = File(savePath, fileName)
        if (!file.exists() || !file.isFile) {
            runBlocking {
                download(url, requestHeaders, savePath, fileName)
            }
        }
        if (file.exists() && file.isFile) {
            val webResourceResponse = WebResourceResponse(
                url.getMimeTypeFromUrl(),
                "UTF-8",
                file.inputStream()
            )
            webResourceResponse.responseHeaders = mapOf("access-control-allow-origin" to "*")
            return webResourceResponse
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun String?.isValidURL(): Boolean {
    if (this.isNullOrBlank()) {
        return false
    }
    val uri: Uri?
    try {
        uri = Uri.parse(this)
    } catch (e: Exception) {
        return false // Invalid URI syntax
    }
    return uri != null && uri.scheme != null && uri.host != null
}

private fun String.getExtensionFromUrl(): String {
    try {
        if (isNotBlank() && this != "null") {
            return MimeTypeMap.getFileExtensionFromUrl(this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
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
        e.printStackTrace()
    }
    return "*/*"
}