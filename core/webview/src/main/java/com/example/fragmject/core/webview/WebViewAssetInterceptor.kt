package com.example.fragmject.core.webview

import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.IOException

/**
 * WebView 本地 assets 资源拦截器。
 *
 * 归属 core:webview，供 article/impl、demo 等模块复用，
 * 避免各业务模块直接依赖 WebView 运行时实现。
 *
 * 路径约定：**URL 路径 = assets 内的相对路径**（直观映射，不做扩展名推断），
 * 例如 `file:///android_asset/html/foo.html` 对应 `assets/html/foo.html`。
 *
 * 路径解析与安全校验统一由 [WebViewCommons.assetPathOf] 承担，
 * 本类只负责按解析结果打开资源，避免两处规则漂移。
 */
object WebViewAssetInterceptor {

    private const val TAG = "WebViewAssetInterceptor"

    fun isAssetsResource(request: WebResourceRequest): Boolean {
        return request.url.toString().startsWith(WebViewCommons.ASSET_PREFIX)
    }

    fun assetsResourceRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        val path = WebViewCommons.assetPathOf(request.url.toString())
        if (path == null) {
            Log.w(TAG, "rejected asset request: ${request.url}")
            return null
        }
        return try {
            val mimeType = request.getMimeTypeFromUrl()
            val encoding = context.assets.open(path)
            WebResourceResponse(mimeType, null, encoding).apply {
                responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
            }
        } catch (e: IOException) {
            Log.e(TAG, "assetsResourceRequest failed: ${request.url}", e)
            null
        }
    }
}
