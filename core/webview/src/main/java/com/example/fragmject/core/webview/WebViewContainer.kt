package com.example.fragmject.core.webview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.fragmject.core.webview.di.WebViewPoolEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

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
private class WebViewCallbacks(
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

private class PooledWebChromeClient(
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
private val WEB_PERMISSIONS = mapOf(
    PermissionRequest.RESOURCE_VIDEO_CAPTURE to Manifest.permission.CAMERA,
    PermissionRequest.RESOURCE_AUDIO_CAPTURE to Manifest.permission.RECORD_AUDIO,
)

private val WEB_PERMISSION_LABELS = mapOf(
    PermissionRequest.RESOURCE_VIDEO_CAPTURE to "摄像头",
    PermissionRequest.RESOURCE_AUDIO_CAPTURE to "麦克风",
)

private fun PermissionRequest.supportedWebResources(): List<String> =
    resources.filter { it in WEB_PERMISSIONS }

/** 仅 https 且带主机名的来源可申请敏感权限；http、file 及无来源一律拒绝。 */
private fun PermissionRequest.hasTrustedOrigin(): Boolean {
    val origin = origin ?: return false
    return origin.scheme.equals("https", ignoreCase = true) && !origin.host.isNullOrBlank()
}

private fun PermissionRequest.answerGrant(resources: List<String>) {
    try {
        grant(resources.toTypedArray())
    } catch (e: Exception) {
        Log.w("WebView", "grant permission failed", e)
    }
}

private fun PermissionRequest.answerDeny() {
    try {
        deny()
    } catch (e: Exception) {
        Log.w("WebView", "deny permission failed", e)
    }
}

private class PooledWebViewClient(
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
        if (request.hasGesture()
            && !request.isRedirect
            && URLUtil.isNetworkUrl(requestUrl)
            && requestUrl != callbacks.url
        ) {
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

private val ALLOWED_DOWNLOAD_SCHEMES = setOf("http", "https")

private fun handleDownload(context: Context, url: String) {
    val uri = url.toUri()
    // URL 来自页面的 DownloadListener，可能是 file:// / content:// / 自定义 scheme；
    // 直接 ACTION_VIEW 会把本地文件或应用私有内容交给外部 App，故只放行 http(s)。
    if (uri.scheme?.lowercase() !in ALLOWED_DOWNLOAD_SCHEMES) {
        Log.w("WebView", "拒绝非 http(s) 下载 URL：$url")
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            // 回调不一定发生在 Activity 上下文，显式 NEW_TASK 避免抛异常
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        Log.e("WebView", "无应用可处理下载链接：$url", e)
    } catch (e: Exception) {
        Log.e("WebView", "setOnDownloadListener: open url failed: $url", e)
    }
}

/**
 * 通用 WebView 控制：reload / evaluateJavascript / 加载进度。
 *
 * 不含任何业务语义；文章调试脚本开关等由业务层自行管理。
 */
@Stable
class WebViewControl(private val scope: CoroutineScope) {

    /**
     * reload 与 evaluateJavascript 拆成两条 Flow：
     * - reload 走 350ms debounce，避免连续点击导致连续刷新；
     * - evaluateJavascript 不防抖，否则连续点击的脚本注入会被丢弃。
     */
    private val reloadEvents: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)
    private val evalEvents: MutableSharedFlow<EvalEvent> =
        MutableSharedFlow(extraBufferCapacity = 8)

    private data class EvalEvent(
        val script: String,
        val resultCallback: ((String) -> Unit)?,
    )

    var progress: Float by mutableFloatStateOf(0f)
        internal set

    @OptIn(FlowPreview::class)
    internal suspend fun handleControlEvents(
        reload: () -> Unit = {},
        evaluateJavascript: (script: String, resultCallback: ((String) -> Unit)?) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.Main) {
        launch {
            reloadEvents.debounce(350.milliseconds).collect { reload() }
        }
        launch {
            evalEvents.collect { event ->
                evaluateJavascript(event.script, event.resultCallback)
            }
        }
    }

    fun reload() {
        scope.launch { reloadEvents.emit(Unit) }
    }

    fun evaluateJavascript(script: String, resultCallback: ((String) -> Unit)? = null) {
        scope.launch { evalEvents.emit(EvalEvent(script, resultCallback)) }
    }
}

@Composable
fun rememberWebViewControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewControl = remember(coroutineScope) { WebViewControl(coroutineScope) }
