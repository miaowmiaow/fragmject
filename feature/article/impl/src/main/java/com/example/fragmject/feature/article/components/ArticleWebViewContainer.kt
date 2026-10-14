package com.example.fragmject.feature.article.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.fragmject.core.webview.JsInjectCache
import com.example.fragmject.core.webview.WebViewContainer
import com.example.fragmject.core.webview.WebViewControl

/**
 * 文章详情页 WebView 业务容器。
 *
 * 复用 core:webview 的通用 [WebViewContainer]，只在此注入文章业务能力：
 * - 通过 [onLongPressImage] 接收长按图片事件；
 * - 按 [injectState] 提供 vconsole 脚本文本，由通用容器执行注入。
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
    val context = LocalContext.current
    WebViewContainer(
        modifier = modifier,
        url = url,
        control = control,
        onTitle = onReceivedTitle,
        onCustomView = onCustomView,
        shouldOverrideUrl = shouldOverrideUrl,
        scriptsProvider = {
            if (injectState) {
                listOf(JsInjectCache.vConsoleJs(context))
            } else {
                emptyList()
            }
        },
        onLongPressImage = onLongPressImage,
    )
}