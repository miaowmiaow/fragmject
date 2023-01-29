package com.example.fragment.library.base.utils

import android.content.Context
import android.graphics.*
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.download
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.encodeUtf8
import java.io.File

object WebViewHelper {

    fun snapshotVisible(webView: WebView, callback: (Bitmap) -> Unit) {
        Thread {
            try {
                webView.isVerticalScrollBarEnabled = false
            } catch (e: Exception) {
                e.printStackTrace()
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
            e.printStackTrace()
        }
        return null
    }

    fun cacheResourceRequest(
        context: Context,
        webRequest: WebResourceRequest
    ): WebResourceResponse? {
        try {
            val url = webRequest.url.toString()
            val cachePath = CacheUtils.getDirPath(context, "web_cache")
            val filePathName = cachePath + File.separator + url.encodeUtf8().md5().hex()
            val file = File(filePathName)
            if (!file.exists() || !file.isFile) {
                runBlocking {
                    download(HttpRequest(url).apply {
                        webRequest.requestHeaders.forEach { putHeader(it.key, it.value) }
                    }, filePathName)
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
            e.printStackTrace()
        }
        return "*/*"
    }

}
