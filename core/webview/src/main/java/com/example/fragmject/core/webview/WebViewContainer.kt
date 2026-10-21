package com.example.fragmject.core.webview

import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 通用 WebView 容器：只负责「显示网页 + 复用 WebView 池 + 拦截资源 + 通用事件回调」，
 * 不包含任何业务语义（文章 / 视频下载 / 调试脚本开关等）。
 *
 * 业务层通过 [scriptsProvider] 提供页面加载进度超过 80% 时注入的脚本文本，
 * 通过 [onLongPressImage] 接收图片地址；原生 WebView 操作只在此模块内执行。
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
    val webViewManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WebViewPoolEntryPoint::class.java,
        ).webViewPool()
    }
    val webResourceCache = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WebViewPoolEntryPoint::class.java,
        ).webResourceCache()
    }

    // 网页权限请求：仅受信来源（https）、仅受支持资源，且每次都弹窗展示来源由用户确认。
    // 每个 PermissionRequest 只能应答一次（grant/deny），因此统一经 answer* 收口。
    // 用 SharedFlow 而不是 mutableState 承接权限请求，避免相同实例引用导致 LaunchedEffect 不再触发
    val permissionRequests =
        remember { MutableSharedFlow<PermissionRequest>(extraBufferCapacity = 1) }
    // 待用户确认 / 正在申请系统权限的请求；同一时刻只处理一个，其余直接拒绝
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    // 用户已点击「允许」，系统权限弹窗进行中
    var permissionConfirmed by remember { mutableStateOf(false) }
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        permissionConfirmed = false
        if (request != null) {
            val allowed = request.supportedWebResources()
            if (allowed.isNotEmpty() && result.values.all { it }) {
                request.answerGrant(allowed)
            } else {
                request.answerDeny()
            }
        }
    }
    LaunchedEffect(permissionRequests) {
        permissionRequests.collect { request ->
            if (pendingPermissionRequest == null &&
                request.hasTrustedOrigin() &&
                request.supportedWebResources().isNotEmpty()
            ) {
                permissionConfirmed = false
                pendingPermissionRequest = request
            } else {
                request.answerDeny()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            pendingPermissionRequest?.answerDeny()
            pendingPermissionRequest = null
        }
    }
    val confirmingRequest = pendingPermissionRequest
    if (confirmingRequest != null && !permissionConfirmed) {
        val resourceNames = confirmingRequest.supportedWebResources()
            .joinToString("、") { WEB_PERMISSION_LABELS.getValue(it) }
        AlertDialog(
            onDismissRequest = {
                pendingPermissionRequest = null
                confirmingRequest.answerDeny()
            },
            title = { Text("网页权限请求") },
            text = { Text("${confirmingRequest.origin?.host.orEmpty()} 请求使用$resourceNames，是否允许？") },
            confirmButton = {
                TextButton(onClick = {
                    permissionConfirmed = true
                    val permissions = confirmingRequest.supportedWebResources()
                        .map { WEB_PERMISSIONS.getValue(it) }
                    requestPermissions.launch(permissions.toTypedArray())
                }) { Text("允许") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPermissionRequest = null
                    confirmingRequest.answerDeny()
                }) { Text("拒绝") }
            },
        )
    }

    // 一份与 Composable 状态绑定的回调容器；WebView 复用时只需替换里面的字段，不会替换 client 本体
    val callbacks = remember {
        WebViewCallbacks(url = url)
    }
    callbacks.url = url
    callbacks.onProgress = { progress ->
        control.progress = progress
        if (progress > 0f) webViewError = null
    }
    callbacks.onTitle = onTitle
    callbacks.onCustomView = onCustomView
    callbacks.onPermissionRequest = { req ->
        if (req != null && !permissionRequests.tryEmit(req)) req.answerDeny()
    }
    // 页面在用户作答前取消了请求：丢弃待处理状态，不再应答（已失效的请求不能 grant/deny）
    callbacks.onPermissionRequestCanceled = { req ->
        if (req != null && pendingPermissionRequest === req) {
            pendingPermissionRequest = null
            permissionConfirmed = false
        }
    }
    callbacks.shouldOverrideUrl = shouldOverrideUrl
    callbacks.scriptsProvider = scriptsProvider
    callbacks.onLongPressImage = onLongPressImage
    callbacks.onReceivedError = { errorCode, description, _ ->
        webViewError = when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP, WebViewClient.ERROR_CONNECT,
            WebViewClient.ERROR_TIMEOUT -> "网络连接失败，请检查网络后重试"

            else -> "加载失败（$errorCode）：$description"
        }
    }

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

    AndroidView(
        factory = { ctx ->
            // 进入 WebView 页面时触发缓存维护（文件数超限按 mtime 淘汰），内部异步执行
            webResourceCache.evictByMtimeIfNeeded(ctx)
            webViewManager.obtain(ctx, url).apply {
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
                if (WebViewCommons.isLoadableUrl(url) && this.url != url) {
                    webResourceCache.prefetchDns(url)
                    this.loadUrl(url)
                }
                tag?.let { title -> callbacks.onTitle(title.toString()) }
                webView = this
            }
        },
        update = { wv ->
            // url 变化时主动 loadUrl，避免复用同一个 WebView 时新地址不生效
            if (WebViewCommons.isLoadableUrl(url) && wv.url != url) {
                webResourceCache.prefetchDns(url)
                wv.loadUrl(url)
            }
        },
        modifier = modifier,
        onRelease = { webViewManager.recycle(it) }
    )

    // 网络错误覆盖层：ERR_CONNECTION_REFUSED 等 WebView 不可达错误在此展示
    val error = webViewError
    if (error != null) {
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
                TextButton(onClick = {
                    webViewError = null
                    webView?.reload()
                }) {
                    Text("点击重试")
                }
            }
        }
    }
}

/**
 * WebView 内部用到的回调集合。WebChromeClient/WebViewClient 提为顶层类，避免在 Composable 工厂里
 * 用匿名内部类闭包当前 Activity / Composable 状态——配合 [WebViewPool] 的复用机制时，
 * 旧的匿名类会跟随 WebView 实例长久存活而泄漏。
 */
