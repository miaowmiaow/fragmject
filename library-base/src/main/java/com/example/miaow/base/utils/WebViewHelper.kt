package com.example.miaow.base.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Looper
import android.util.Base64
import android.util.Log
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
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.encodeUtf8
import java.io.File

object WebViewHelper {

    fun setDownloadListener(webView: WebView) {
        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                webView.context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
            }
        }
    }

    fun setOnLongClickListener(webView: WebView) {
        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            when (result.type) {
                WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    result.extra?.let { extra ->
                        val contextWrapper = webView.context as MutableContextWrapper
                        contextWrapper.baseContext.saveImagesBase64Dialog(extra)
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun Context.saveImagesBase64Dialog(data: String) {
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

    fun goBack(webView: WebView, originalUrl: String): Boolean {
        val canBack = webView.canGoBack()
        if (canBack) webView.goBack()
        val backForwardList = webView.copyBackForwardList()
        val currentIndex = backForwardList.currentIndex
        if (currentIndex == 0) {
            val currentUrl = backForwardList.currentItem?.url
            val currentHost = Uri.parse(currentUrl).host
            //栈底不是链接则直接返回
            if (currentHost.isNullOrBlank()) return false
            //栈底链接不是原始链接则直接返回
            if (originalUrl != currentUrl) return false
        }
        return canBack
    }

    fun snapshotVisible(webView: WebView, callback: (Bitmap) -> Unit) {
        Thread {
            try {
                webView.isVerticalScrollBarEnabled = false
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
            }
            var contentHeight = webView.contentHeight
            webView.measure(0, 0)
            val measuredHeight = webView.measuredHeight
            if (contentHeight > webView.height && measuredHeight > contentHeight) {
                contentHeight = measuredHeight
            }
            val saveBitmap =
                Bitmap.createBitmap(webView.width, contentHeight, Bitmap.Config.ARGB_8888)
            val tempBitmap =
                Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas()
            val paint = Paint()
            val src = Rect()//代表图片矩形范围
            val des = RectF()//代表Canvas的矩形范围(显示位置)
            var scrollY = 0f
            while (scrollY < contentHeight) {
                canvas.setBitmap(tempBitmap)
                webView.scrollTo(0, scrollY.toInt())
                webView.draw(canvas)
                Thread.sleep(50)
                val top = scrollY
                scrollY += webView.height
                if (scrollY > contentHeight) {
                    val surplusY = webView.height - (scrollY - contentHeight)
                    src.set(
                        0,
                        (tempBitmap.height - surplusY).toInt(),
                        tempBitmap.width,
                        tempBitmap.height
                    )
                    des.set(0f, top, tempBitmap.width.toFloat(), top + surplusY)
                } else {
                    src.set(0, 0, tempBitmap.width, tempBitmap.height)
                    des.set(0f, top, tempBitmap.width.toFloat(), top + tempBitmap.height.toFloat())
                }
                canvas.setBitmap(saveBitmap)
                canvas.drawBitmap(tempBitmap, src, des, paint)
            }
            callback.invoke(saveBitmap)
        }.start()
    }

    fun isAssetsResource(webRequest: WebResourceRequest): Boolean {
        val url = webRequest.url.toString()
        return url.startsWith("file:///android_asset/")
    }

    fun isCacheResource(webRequest: WebResourceRequest): Boolean {
        val url = webRequest.url.toString()
        val extension = getExtensionFromUrl(url)
        return extension == "ico" || extension == "bmp" || extension == "gif"
                || extension == "jpeg" || extension == "jpg" || extension == "png"
                || extension == "svg" || extension == "webp" || extension == "css"
                || extension == "js" || extension == "json" || extension == "eot"
                || extension == "otf" || extension == "ttf" || extension == "woff"
    }

    fun assetsResourceRequest(
        context: Context,
        webRequest: WebResourceRequest
    ): WebResourceResponse? {
        try {
            val url = webRequest.url.toString()
            val filenameIndex = url.lastIndexOf("/") + 1
            val filename = url.substring(filenameIndex)
            val suffixIndex = url.lastIndexOf(".")
            val suffix = url.substring(suffixIndex + 1)
            val webResourceResponse = WebResourceResponse(
                getMimeTypeFromUrl(url),
                "UTF-8",
                context.assets.open("$suffix/$filename")
            )
            webResourceResponse.responseHeaders = mapOf("access-control-allow-origin" to "*")
            return webResourceResponse
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return null
    }

    fun cacheResourceRequest(
        context: Context,
        webRequest: WebResourceRequest
    ): WebResourceResponse? {
        try {
            val url = webRequest.url.toString()
            val savePath = CacheUtils.getDirPath(context, "web_cache")
            val fileName = url.encodeUtf8().md5().hex()
            val file = File(savePath, fileName)
            if (!file.exists() || !file.isFile) {
                runBlocking {
                    download(url, webRequest.requestHeaders, savePath, fileName)
                }
            }
            if (file.exists() && file.isFile) {
                val webResourceResponse = WebResourceResponse(
                    getMimeTypeFromUrl(url),
                    "UTF-8",
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

    private fun getExtensionFromUrl(url: String): String {
        try {
            if (url.isNotBlank() && url != "null") {
                return MimeTypeMap.getFileExtensionFromUrl(url)
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return ""
    }

    private fun getMimeTypeFromUrl(url: String): String {
        try {
            val extension = getExtensionFromUrl(url)
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

@SuppressLint("SetJavaScriptEnabled")
class WebViewManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: WebViewManager? = null

        private fun instance() = INSTANCE ?: synchronized(this) {
            INSTANCE ?: WebViewManager().also { INSTANCE = it }
        }

        fun prepare(context: Context) {
            instance().prepare(context)
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
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
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
        webView.clearHistory()
        webView.resumeTimers()
        prepare(context.applicationContext)
        return webView
    }

    fun recycle(webView: WebView) {
        try {
            webView.stopLoading()
            webView.loadDataWithBaseURL("about:blank", "", "text/html", "utf-8", null)
            webView.clearHistory()
            webView.pauseTimers()
            val parent = webView.parent
            if (parent != null) {
                (parent as ViewGroup).removeView(webView)
            }
            val contextWrapper = webView.context as MutableContextWrapper
            contextWrapper.baseContext = webView.context.applicationContext
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
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
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

}