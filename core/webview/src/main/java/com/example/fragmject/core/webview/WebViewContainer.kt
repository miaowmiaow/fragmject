package com.example.fragmject.core.webview

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fragmject.core.webview.di.WebViewPoolEntryPoint
import dagger.hilt.android.EntryPointAccessors

/** 容器向 WebView 暴露的 UI 回调；打包后避免辅助函数参数表继续膨胀。 */
@Stable
private class WebViewUiCallbacks(
    val onTitle: (String?) -> Unit,
    val onCustomView: (View?) -> Unit,
    val shouldOverrideUrl: (String) -> Unit,
    val scriptsProvider: () -> List<String>,
    val onLongPressImage: ((String?) -> Unit)?,
)

/**
 * 通用 WebView 容器：只负责「显示网页 + 复用 WebView 池 + 拦截资源 + 通用事件回调」，
 * 不包含任何业务语义（文章 / 视频下载 / 调试脚本开关等）。
 *
 * 业务层通过 [scriptsProvider] 提供页面加载进度超过 80% 时注入的脚本文本，
 * 通过 [onLongPressImage] 接收图片地址；原生 WebView 操作只在此模块内执行。
 *
 * 权限请求、回调装配、AndroidView 装配与错误覆盖层各自独立，主体只做串联。
 */
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    url: String,
    control: WebViewControl,
    onTitle: (String?) -> Unit = {},
    onCustomView: (View?) -> Unit = {},
    shouldOverrideUrl: (String) -> Unit = {},
    scriptsProvider: () -> List<String> = { emptyList() },
    onLongPressImage: ((String?) -> Unit)? = null,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var webViewError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WebViewPoolEntryPoint::class.java,
        )
    }
    val webViewPool = remember(entryPoint) { entryPoint.webViewPool() }
    val webResourceCache = remember(entryPoint) { entryPoint.webResourceCache() }
    val permissionHost = rememberWebPermissionHost()
    val callbacks = rememberWebViewCallbacks(
        url = url,
        control = control,
        permissionHost = permissionHost,
        ui = remember(onTitle, onCustomView, shouldOverrideUrl, scriptsProvider, onLongPressImage) {
            WebViewUiCallbacks(onTitle, onCustomView, shouldOverrideUrl, scriptsProvider, onLongPressImage)
        },
        onError = { webViewError = it },
    )

    LaunchedEffect(webView, control) {
        webView?.let {
            with(control) {
                handleControlEvents(
                    reload = { it.reload() },
                    evaluateJavascript = { script, resultCallback ->
                        it.evaluateJavascript(script) { value ->
                            resultCallback?.invoke(value ?: "")
                        }
                    }
                )
            }
        }
    }

    WebViewHost(
        webViewPool = webViewPool,
        webResourceCache = webResourceCache,
        url = url,
        callbacks = callbacks,
        modifier = modifier,
        onCreated = { webView = it },
    )
    WebPermissionRequestDialog(permissionHost)
    WebViewErrorOverlay(
        error = webViewError,
        onRetry = {
            webViewError = null
            webView?.reload()
        },
    )
}

/**
 * 装配 [WebViewCallbacks]。
 *
 * WebChromeClient/WebViewClient 提为顶层类，避免在 Composable 工厂里用匿名内部类闭包当前
 * Activity / Composable 状态——配合 [WebViewPool] 的复用机制时，旧的匿名类会跟随 WebView
 * 实例长久存活而泄漏。
 */
@Composable
private fun rememberWebViewCallbacks(
    url: String,
    control: WebViewControl,
    permissionHost: WebPermissionHost,
    ui: WebViewUiCallbacks,
    onError: (String?) -> Unit,
): WebViewCallbacks {
    // 一份与 Composable 状态绑定的回调容器；WebView 复用时只需替换里面的字段，不会替换 client 本体
    val callbacks = remember { WebViewCallbacks(url = url) }
    callbacks.url = url
    callbacks.onProgress = { progress ->
        control.progress = progress
        if (progress > 0f) onError(null)
    }
    callbacks.onTitle = ui.onTitle
    callbacks.onCustomView = ui.onCustomView
    callbacks.onPermissionRequest = { req ->
        if (req != null && !permissionHost.requests.tryEmit(req)) req.answerDeny()
    }
    // 页面在用户作答前取消了请求：丢弃待处理状态，不再应答（已失效的请求不能 grant/deny）
    callbacks.onPermissionRequestCanceled = permissionHost::cancel
    callbacks.shouldOverrideUrl = ui.shouldOverrideUrl
    callbacks.scriptsProvider = ui.scriptsProvider
    callbacks.onLongPressImage = ui.onLongPressImage
    callbacks.onReceivedError = { errorCode, description, _ ->
        onError(webViewErrorMessage(errorCode, description))
    }
    return callbacks
}

/** 错误码 → 用户可读文案。 */
private fun webViewErrorMessage(errorCode: Int, description: String): String = when (errorCode) {
    WebViewClient.ERROR_HOST_LOOKUP, WebViewClient.ERROR_CONNECT,
    WebViewClient.ERROR_TIMEOUT -> "网络连接失败，请检查网络后重试"

    else -> "加载失败（$errorCode）：$description"
}

/** 池化 WebView 的宿主；创建/复用/回收都在这里完成。 */
@Composable
private fun WebViewHost(
    webViewPool: WebViewPool,
    webResourceCache: WebResourceCache,
    url: String,
    callbacks: WebViewCallbacks,
    modifier: Modifier,
    onCreated: (WebView) -> Unit,
) {
    AndroidView(
        factory = { ctx ->
            // 进入 WebView 页面时触发缓存维护（文件数超限按 mtime 淘汰），内部异步执行
            webResourceCache.evictByMtimeIfNeeded(ctx)
            webViewPool.obtain(ctx, url).apply {
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    handleDownload(ctx, downloadUrl)
                }
                setOnLongClickListener {
                    val onImage = callbacks.onLongPressImage
                    val result = hitTestResult
                    if (onImage != null && WebViewCommons.isImageHitTest(result)) {
                        onImage(result.extra)
                        true
                    } else {
                        false
                    }
                }
                val chromeClient = PooledWebChromeClient(callbacks)
                webChromeClient = chromeClient
                webViewClient =
                    PooledWebViewClient(callbacks, webResourceCache) { chromeClient.resetInjection() }
                loadUrlIfNeeded(this, url, webResourceCache)
                tag?.let { title -> callbacks.onTitle(title.toString()) }
                onCreated(this)
            }
        },
        update = { wv -> loadUrlIfNeeded(wv, url, webResourceCache) },
        modifier = modifier,
        onRelease = { webViewPool.recycle(it) }
    )
}

/** url 变化时主动 loadUrl，避免复用同一个 WebView 时新地址不生效。 */
private fun loadUrlIfNeeded(webView: WebView, url: String, webResourceCache: WebResourceCache) {
    if (WebViewCommons.isLoadableUrl(url) && webView.url != url) {
        webResourceCache.prefetchDns(url)
        webView.loadUrl(url)
    }
}

/** 网络错误覆盖层：ERR_CONNECTION_REFUSED 等 WebView 不可达错误在此展示。 */
@Composable
private fun WebViewErrorOverlay(error: String?, onRetry: () -> Unit) {
    if (error == null) return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRetry) {
                Text("点击重试")
            }
        }
    }
}
