package com.example.fragmject.feature.article.utils

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.File

/**
 * WebView 本地 assets 资源拦截器。
 *
 * 从 [WebViewManager] 抽离到 article/api，供 article/impl 内部与 demo 等模块复用，
 * 避免 demo 直接依赖 article/impl（仅需依赖 article/api 即可）。
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
