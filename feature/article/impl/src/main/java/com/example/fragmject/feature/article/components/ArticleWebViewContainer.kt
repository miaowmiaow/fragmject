package com.example.fragmject.feature.article.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fragmject.core.webview.JsInjectCache
import com.example.fragmject.core.webview.WebViewCommons
import com.example.fragmject.core.webview.WebViewContainer
import com.example.fragmject.core.webview.WebViewControl

/**
 * 文章详情页 WebView 业务容器。
 *
 * 复用 core:webview 的通用 [WebViewContainer]，只在此注入文章业务能力：
 * - 通过 [onWebViewCreated] 注册长按图片监听；
 * - 通过 [onInjectScripts] 注入 vconsole 等文章脚本。
 *
 * [injectState] 控制是否注入 vconsole 调试脚本，由调用方（WebScreen）持有该开关状态。
 */
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
) {
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
        },
        onWebViewCreated = { webView ->
            webView.setOnLongClickListener {
                val result = webView.hitTestResult
                if (WebViewCommons.isImageHitTest(result)) {
                    onLongPressImage(result.extra)
                    true
                } else {
                    false
                }
            }
        },
    )
}