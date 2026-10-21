package com.example.fragmject.core.webview

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebView 池化用的 Client 实现：ChromeClient 负责权限注入，WebViewClient 负责资源拦截与下载。
 */
internal class WebViewCallbacks(
    var url: String,
    var onProgress: (Float) -> Unit = {},
    var onTitle: (String?) -> Unit = {},
    var onCustomView: (View?) -> Unit = {},
    var onPermissionRequest: (PermissionRequest?) -> Unit = {},
    var onPermissionRequestCanceled: (PermissionRequest?) -> Unit = {},
    var shouldOverrideUrl: (String) -> Unit = {},
    var scriptsProvider: () -> List<String> = { emptyList() },
    var onLongPressImage: ((String?) -> Unit)? = null,
    var onReceivedError: (errorCode: Int, description: String, failingUrl: String?) -> Unit = { _, _, _ -> },
)

internal class PooledWebChromeClient(
    private val callbacks: WebViewCallbacks,
) : WebChromeClient() {

    /** 每个 url 仅注入一次脚本，避免 onProgress 反复回调时重复注入。 */
    private var injectedForUrl: String? = null

    fun resetInjection() {
        injectedForUrl = null
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        callbacks.onProgress((newProgress / 100f).coerceIn(0f, 1f))
        if (newProgress > 80 && injectedForUrl != view.url) {
            callbacks.scriptsProvider().forEach { script ->
                view.evaluateJavascript(script) {}
            }
            injectedForUrl = view.url
        }
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        callbacks.onTitle(title)
        view?.tag = title
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        callbacks.onCustomView(view)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        callbacks.onCustomView(null)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        callbacks.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        callbacks.onPermissionRequestCanceled(request)
    }
}

/** 网页可申请的资源与对应系统权限；不在表内的资源（如 DRM、MIDI）一律不授予。 */

internal class PooledWebViewClient(
    private val callbacks: WebViewCallbacks,
    private val webResourceCache: WebResourceCache,
    private val onReset: () -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (view != null && request != null) {
            val context = view.context
            val response: WebResourceResponse? = when {
                webResourceCache.isCacheableHtml(request) ->
                    webResourceCache.cacheHtmlRequest(context, request)

                WebViewAssetInterceptor.isAssetsResource(request) ->
                    WebViewAssetInterceptor.assetsResourceRequest(context, request)

                webResourceCache.isCacheableResource(request) ->
                    webResourceCache.cacheResourceRequest(context, request, callbacks.url)

                else -> null
            }
            if (response != null) return response
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        if (view == null || request == null) return false
        val requestUrl = request.url.toString()
        val isUserNavigation = request.hasGesture() &&
            !request.isRedirect &&
            URLUtil.isNetworkUrl(requestUrl)
        if (isUserNavigation && requestUrl != callbacks.url) {
            callbacks.shouldOverrideUrl(requestUrl)
            return true
        }
        if (!URLUtil.isValidUrl(requestUrl)) {
            try {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
            } catch (e: Exception) {
                Log.e("WebView", "shouldOverrideUrlLoading failed: ${request.url}", e)
            }
            return true
        }
        return false
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onReset()
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        if (request?.isForMainFrame == true && error != null) {
            callbacks.onReceivedError(
                error.errorCode,
                error.description?.toString() ?: "未知错误",
                request.url?.toString(),
            )
        }
    }
}

