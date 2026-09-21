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
import android.webkit.ValueCallback
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * 通用 WebView 容器：只负责「显示网页 + 复用 WebView 池 + 拦截资源 + 通用事件回调」，
 * 不包含任何业务语义（文章 / 视频下载 / 脚本注入等）。
 *
 * 业务层（如 article）通过 [onWebViewCreated] 注册 JS 接口、长按监听等自身能力，
 * 通过 [onInjectScripts] 在页面达到注入阈值时注入自己的脚本。
 */
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    url: String,
    control: WebViewControl,
    onTitle: (String?) -> Unit = {},
    onCustomView: (View?) -> Unit = {},
    shouldOverrideUrl: (String) -> Unit = {},
    onInjectScripts: (WebView) -> Unit = {},
    onWebViewCreated: (WebView) -> Unit = {},
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

    // 用 SharedFlow 而不是 mutableState 承接权限请求，避免相同实例引用导致 LaunchedEffect 不再触发
    val permissionRequests =
        remember { MutableSharedFlow<PermissionRequest>(extraBufferCapacity = 1) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val resourceToPermissionMap = remember {
        mapOf(
            "android.webkit.resource.VIDEO_CAPTURE" to Manifest.permission.CAMERA,
            "android.webkit.resource.AUDIO_CAPTURE" to Manifest.permission.RECORD_AUDIO,
        )
    }
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        pendingPermissionRequest?.apply {
            if (result.values.all { it }) grant(resources)
        }
        pendingPermissionRequest = null
    }
    LaunchedEffect(permissionRequests) {
        permissionRequests.collectLatest { request ->
            pendingPermissionRequest = request
            val permissions = request.resources.mapNotNull { resourceToPermissionMap[it] }
            if (permissions.isNotEmpty()) {
                requestPermissions.launch(permissions.toTypedArray())
            }
        }
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
    callbacks.onPermissionRequest = { req -> req?.let { permissionRequests.tryEmit(it) } }
    callbacks.shouldOverrideUrl = shouldOverrideUrl
    callbacks.onInjectScripts = onInjectScripts
    callbacks.onWebViewCreated = onWebViewCreated
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
                            resultCallback?.onReceiveValue(value ?: "")
                        }
                    }
                )
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            webViewManager.obtain(ctx, url).apply {
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    handleDownload(ctx, downloadUrl)
                }
                // 业务钩子：注册 JS 接口、设置长按监听等业务能力
                callbacks.onWebViewCreated(this)
                val chromeClient = PooledWebChromeClient(callbacks)
                webChromeClient = chromeClient
                webViewClient =
                    PooledWebViewClient(callbacks, webViewManager) { chromeClient.resetInjection() }
                if (URLUtil.isValidUrl(url) && this.url != url) {
                    webViewManager.prefetchDns(url)
                    this.loadUrl(url)
                }
                tag?.let { title -> callbacks.onTitle(title.toString()) }
                webView = this
            }
        },
        update = { wv ->
            // url 变化时主动 loadUrl，避免复用同一个 WebView 时新地址不生效
            if (URLUtil.isValidUrl(url) && wv.url != url) {
                webViewManager.prefetchDns(url)
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
    var shouldOverrideUrl: (String) -> Unit = {},
    var onInjectScripts: (WebView) -> Unit = {},
    var onWebViewCreated: (WebView) -> Unit = {},
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
            callbacks.onInjectScripts(view)
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
}

private class PooledWebViewClient(
    private val callbacks: WebViewCallbacks,
    private val webViewManager: WebViewPool,
    private val onReset: () -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (view != null && request != null) {
            val context = view.context
            when {
                WebViewAssetInterceptor.isAssetsResource(request) ->
                    return WebViewAssetInterceptor.assetsResourceRequest(context, request)

                webViewManager.isCacheResource(request) ->
                    return webViewManager.cacheResourceRequest(context, request)
            }
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

private fun handleDownload(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        context.startActivity(intent)
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
        val resultCallback: ValueCallback<String>?,
    )

    var progress: Float by mutableFloatStateOf(0f)
        internal set

    @OptIn(FlowPreview::class)
    internal suspend fun handleControlEvents(
        reload: () -> Unit = {},
        evaluateJavascript: (script: String, resultCallback: ValueCallback<String>?) -> Unit = { _, _ -> },
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

    fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>? = null) {
        scope.launch { evalEvents.emit(EvalEvent(script, resultCallback)) }
    }
}

@Composable
fun rememberWebViewControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewControl = remember(coroutineScope) { WebViewControl(coroutineScope) }
