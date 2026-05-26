package com.example.fragment.project.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.fragment.project.components.StandardDialog
import com.example.miaow.base.utils.injectQuickVideoJs
import com.example.miaow.base.utils.injectVConsoleJs
import com.example.miaow.base.utils.saveImagesToAlbum
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
 * WebView 内部用到的回调集合。WebChromeClient/WebViewClient 提为顶层类，避免在 Composable 工厂里
 * 用匿名内部类闭包当前 Activity / Composable 状态——配合 [WebViewManager] 的复用机制时，
 * 旧的匿名类会跟随 WebView 实例长久存活而泄漏。
 */
private class WebViewCallbacks(
    var url: String,
    var onProgress: (Float) -> Unit = {},
    var onTitle: (String?) -> Unit = {},
    var onCustomView: (View?) -> Unit = {},
    var onPermissionRequest: (PermissionRequest?) -> Unit = {},
    var shouldOverrideUrl: (String) -> Unit = {},
    var injectScript: (String) -> Unit = {},
    var injectVConsole: () -> Boolean = { false },
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
            if (callbacks.injectVConsole()) {
                callbacks.injectScript("vconsole")
            }
            callbacks.injectScript("quickVideo")
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
    private val onReset: () -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (view != null && request != null) {
            val context = view.context
            when {
                WebViewManager.isCacheResource(request) ->
                    return WebViewManager.cacheResourceRequest(context, request)

                WebViewManager.isAssetsResource(request) ->
                    return WebViewManager.assetsResourceRequest(context, request)
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
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    url: String,
    control: WebViewControl,
    modifier: Modifier = Modifier,
    onReceivedTitle: (title: String?) -> Unit = {},
    onCustomView: (view: View?) -> Unit = {},
    shouldOverrideUrl: (url: String) -> Unit = {},
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var extra by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 用 SharedFlow 而不是 mutableState 承接权限请求，避免相同实例引用导致 LaunchedEffect 不再触发
    val permissionRequests = remember { MutableSharedFlow<PermissionRequest>(extraBufferCapacity = 1) }
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
    callbacks.onProgress = { control.progress = it }
    callbacks.onTitle = onReceivedTitle
    callbacks.onCustomView = onCustomView
    callbacks.onPermissionRequest = { req -> req?.let { permissionRequests.tryEmit(it) } }
    callbacks.shouldOverrideUrl = shouldOverrideUrl
    callbacks.injectVConsole = { control.injectState }
    callbacks.injectScript = { tag ->
        webView?.let { wv ->
            val script = when (tag) {
                "vconsole" -> wv.context.injectVConsoleJs()
                "quickVideo" -> wv.context.injectQuickVideoJs()
                else -> return@let
            }
            wv.evaluateJavascript(script) {}
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
            WebViewManager.obtain(ctx, url).apply {
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    handleDownload(ctx, downloadUrl)
                }
                setOnLongClickListener {
                    val result = hitTestResult
                    when (result.type) {
                        WebView.HitTestResult.IMAGE_TYPE,
                        WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                            extra = result.extra
                            showDialog = true
                            true
                        }

                        else -> false
                    }
                }
                val chromeClient = PooledWebChromeClient(callbacks)
                webChromeClient = chromeClient
                webViewClient = PooledWebViewClient(callbacks) { chromeClient.resetInjection() }
                if (URLUtil.isValidUrl(url) && this.url != url) {
                    this.loadUrl(url)
                }
                tag?.let { title -> onReceivedTitle(title.toString()) }
                webView = this
            }
        },
        update = { wv ->
            // url 变化时主动 loadUrl，避免复用同一个 WebView 时新地址不生效
            if (URLUtil.isValidUrl(url) && wv.url != url) {
                wv.loadUrl(url)
            }
        },
        modifier = modifier,
        onRelease = { WebViewManager.recycle(it) }
    )

    StandardDialog(
        show = showDialog,
        title = "提示",
        text = "你希望保存该图片吗？",
        onConfirm = {
            extra?.let {
                if (URLUtil.isValidUrl(it)) {
                    context.saveImagesToAlbum(it) { _, _ ->
                        Toast.makeText(context, "保存图片成功", Toast.LENGTH_SHORT).show()
                        showDialog = false
                    }
                } else {
                    var str = it
                    if (str.contains(",")) {
                        str = str.split(",")[1]
                    }
                    val array = Base64.decode(str, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(array, 0, array.size)
                    context.saveImagesToAlbum(bitmap) { _, _ ->
                        Toast.makeText(context, "保存图片成功", Toast.LENGTH_SHORT).show()
                        showDialog = false
                    }
                }
            }
        },
        onDismiss = { showDialog = false },
    )
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

@Stable
class WebViewControl(private val scope: CoroutineScope) {

    /**
     * reload 与 evaluateJavascript 拆成两条 Flow：
     * - reload 走 350ms debounce，避免连续点击导致连续刷新；
     * - evaluateJavascript 不防抖，否则连续点击的脚本注入会被丢弃。
     */
    private val reloadEvents: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)
    private val evalEvents: MutableSharedFlow<EvalEvent> = MutableSharedFlow(extraBufferCapacity = 8)

    private data class EvalEvent(
        val script: String,
        val resultCallback: ValueCallback<String>?,
    )

    var injectState: Boolean by mutableStateOf(false)
        internal set
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

    fun inject(): Boolean {
        injectState = !injectState
        reload()
        return injectState
    }

    fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>? = null) {
        scope.launch { evalEvents.emit(EvalEvent(script, resultCallback)) }
    }
}

@Composable
fun rememberWebViewControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewControl = remember(coroutineScope) { WebViewControl(coroutineScope) }
