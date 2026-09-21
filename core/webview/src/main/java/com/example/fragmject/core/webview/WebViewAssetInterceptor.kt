package com.example.fragmject.core.webview

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.File

/**
 * WebView 本地 assets 资源拦截器。
 *
 * 归属 core:webview，供 article/impl、demo 等模块复用，
 * 避免各业务模块直接依赖 WebView 运行时实现。
 */
object WebViewAssetInterceptor {

    private const val TAG = "WebViewAssetInterceptor"

    fun isAssetsResource(request: WebResourceRequest): Boolean {
        return request.url.toString().startsWith("file:///android_asset/")
    }

    fun assetsResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val filename = url.substringAfterLast("/")
            val suffix = url.substringAfterLast(".")
            val mimeType = request.getMimeTypeFromUrl()
            val encoding = context.assets.open(suffix + File.separator + filename)
            WebResourceResponse(mimeType, null, encoding).apply {
                responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
            }
        } catch (e: Exception) {
            Log.e(TAG, "assetsResourceRequest failed: ${request.url}", e)
            null
        }
    }

    private fun WebResourceRequest.getExtensionFromUrl(): String {
        return try {
            MimeTypeMap.getFileExtensionFromUrl(url.toString())
        } catch (e: Exception) {
            Log.e(TAG, "getExtensionFromUrl failed: $url", e)
            ""
        }
    }

    private fun WebResourceRequest.getMimeTypeFromUrl(): String {
        return try {
            when (val extension = getExtensionFromUrl()) {
                "", "null", "*/*" -> "*/*"
                "json" -> "application/json"
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMimeTypeFromUrl failed: $url", e)
            "*/*"
        }
    }
}
