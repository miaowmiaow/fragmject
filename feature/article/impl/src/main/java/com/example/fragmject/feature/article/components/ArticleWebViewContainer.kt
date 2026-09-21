@file:SuppressLint("JavascriptInterface")

package com.example.fragmject.feature.article.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView as AndroidWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.fragmject.core.webview.JsInjectCache
import com.example.fragmject.core.webview.WebViewContainer
import com.example.fragmject.core.webview.WebViewControl

/**
 * JS → Native 视频保存桥接。
 *
 * 注册到 [AndroidWebView.addJavascriptInterface] 中供 H5 的
 * `VideoSaveBridge.onVideoLongPress(src)` 调用。
 * 所有业务逻辑（空 URL / 单视频 / 多视频分发）由 [onResult] lambda 委托给 Composable 层处理，
 * 本类只负责解析 `|` 分隔的 URL 列表并回调。
 */
@SuppressLint("JavascriptInterface")
private class VideoSaveBridge(
    private val onResult: (urls: List<String>) -> Unit,
) {
    @JavascriptInterface
    fun onVideoLongPress(videoUrl: String) {
        if (videoUrl.isEmpty()) {
            onResult(emptyList())
            return
        }
        onResult(videoUrl.split("|").filter { it.isNotBlank() })
    }
}

/**
 * 文章详情页 WebView 业务容器。
 *
 * 复用 core:webview 的通用 [WebViewContainer]，只在此注入文章业务能力：
 * - 通过 [onWebViewCreated] 注册视频保存 JS 接口、长按图片监听；
 * - 通过 [onInjectScripts] 注入 vconsole / quickVideo / videoSave 等文章脚本。
 *
 * [injectState] 控制是否注入 vconsole 调试脚本，由调用方（WebScreen）持有该开关状态。
 */
@SuppressLint("JavascriptInterface")
@Composable
fun ArticleWebViewContainer(
    modifier: Modifier = Modifier,
    url: String,
    control: WebViewControl,
    injectState: Boolean = false,
    onReceivedTitle: (String?) -> Unit = {},
    onCustomView: (View?) -> Unit = {},
    shouldOverrideUrl: (String) -> Unit = {},
    onLongPressImage: (String?) -> Unit = {},
    onVideoDetected: (List<String>) -> Unit = {},
) {
    val videoBridge = remember { VideoSaveBridge { urls -> onVideoDetected(urls) } }

    WebViewContainer(
        modifier = modifier,
        url = url,
        control = control,
        onTitle = onReceivedTitle,
        onCustomView = onCustomView,
        shouldOverrideUrl = shouldOverrideUrl,
        onInjectScripts = { webView ->
            if (injectState) {
                webView.evaluateJavascript(JsInjectCache.vConsoleJs(webView.context)) {}
            }
            webView.evaluateJavascript(JsInjectCache.quickVideoJs(webView.context)) {}
            webView.evaluateJavascript(JsInjectCache.videoSaveJs()) {}
        },
        onWebViewCreated = { webView ->
            webView.addJavascriptInterface(videoBridge, "VideoSaveBridge")
            webView.setOnLongClickListener {
                val result = webView.hitTestResult
                when (result.type) {
                    AndroidWebView.HitTestResult.IMAGE_TYPE,
                    AndroidWebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        onLongPressImage(result.extra)
                        true
                    }

                    else -> false
                }
            }
        },
    )
}