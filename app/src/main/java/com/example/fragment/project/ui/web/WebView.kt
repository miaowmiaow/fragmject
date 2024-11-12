package com.example.fragment.project.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.miaow.base.utils.injectVConsoleJs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    url: String,
    navigator: WebViewNavigator,
    modifier: Modifier = Modifier,
    goBack: (url: String) -> Unit = {},
    goForward: (url: String) -> Unit = {},
    navigateUp: () -> Unit = {},
    onReceivedTitle: (url: String?, title: String?) -> Unit = { _, _ -> },
    shouldOverrideUrl: (url: String) -> Unit = {},
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var fullScreenLayer by remember { mutableStateOf<View?>(null) }
    var injectState by remember { mutableStateOf(false) }
    BackHandler(true) {
        navigator.navigateBack()
    }
    LaunchedEffect(webView, navigator) {
        webView?.let {
            with(navigator) {
                handleNavigationEvents(
                    onBack = {
                        if (it.canGoBack()) {
                            it.goBack()
                        } else {
                            val backUrl = WebViewManager.back(it)
                            if (backUrl.isNotBlank()) {
                                goBack(backUrl)
                            } else {
                                navigateUp()
                            }
                        }
                    },
                    onForward = {
                        val forwardUrl = WebViewManager.forward(it)
                        if (forwardUrl.isNotBlank()) {
                            goForward(forwardUrl)
                        }
                    },
                    reload = {
                        it.reload()
                    }
                )
            }
            navigator.loadedUrl = it.url
        }
    }

    val resourceToPermissionMap = mapOf(
        "android.webkit.resource.VIDEO_CAPTURE" to Manifest.permission.CAMERA,
        "android.webkit.resource.AUDIO_CAPTURE" to Manifest.permission.RECORD_AUDIO
    )
    var permissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val requestPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            permissionRequest?.apply {
                var isGranted = true
                result.entries.forEach { entry ->
                    if (!entry.value) {
                        isGranted = false
                    }
                }
                if (isGranted) {
                    grant(resources)
                }
            }
        }
    LaunchedEffect(permissionRequest) {
        val permissions = mutableListOf<String>()
        permissionRequest?.resources?.forEach { resource ->
            resourceToPermissionMap[resource]?.let { permission ->
                permissions.add(permission)
            }
        }
        permissions.toTypedArray().apply {
            requestPermissions.launch(this)
        }
    }
    AndroidView(
        factory = { context ->
            val activity = context as ComponentActivity
            val windowManager = activity.windowManager
            WebViewManager.obtain(context, url).apply {
                setDownloadListener()
                setOnLongClickListener()
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webChromeClient = object : WebChromeClient() {

                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        navigator.progress = (newProgress / 100f).coerceIn(0f, 1f)
                        if (newProgress > 80 && navigator.injectVConsole && !injectState) {
                            view.apply { evaluateJavascript(context.injectVConsoleJs()) {} }
                            injectState = true
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        if (view == null) {
                            return
                        }
                        navigator.title = title
                        onReceivedTitle(view.url, title)
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        super.onShowCustomView(view, callback)
                        windowManager.addView(
                            view,
                            WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION)
                        )
                        fullScreenLayer = view
                    }

                    override fun onHideCustomView() {
                        super.onHideCustomView()
                        fullScreenLayer?.let {
                            windowManager.removeViewImmediate(fullScreenLayer)
                            fullScreenLayer = null
                        }
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        permissionRequest = request
                    }
                }
                webViewClient = object : WebViewClient() {

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (view != null && request != null) {
                            when {
                                request.isAssetsResource() -> {
                                    return request.assetsResourceRequest(view.context)
                                }

                                request.isCacheResource() -> {
                                    return request.cacheResourceRequest(view.context)
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        if (view == null || request == null) {
                            return false
                        }
                        val requestUrl = request.url.toString()
                        if (request.hasGesture()
                            && !request.isRedirect
                            && URLUtil.isNetworkUrl(requestUrl)
                            && requestUrl != url
                        ) {
                            shouldOverrideUrl(requestUrl)
                            return true
                        }
                        if (!URLUtil.isValidUrl(requestUrl)) {
                            try {
                                view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                            } catch (e: Exception) {
                                Log.e(this.javaClass.name, e.message.toString())
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        navigator.loadedUrl = url
                        injectState = false
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        injectState = false
                    }
                }
                if (URLUtil.isValidUrl(url) && !URLUtil.isValidUrl(this.url)) {
                    this.loadUrl(url)
                }
            }.also { webView = it }
        },
        modifier = modifier,
        onRelease = {
            WebViewManager.recycle(it)
        }
    )
}

@Stable
class WebViewNavigator(
    private val scope: CoroutineScope
) {
    private sealed interface NavigationEvent {
        data object Back : NavigationEvent
        data object Forward : NavigationEvent
        data object Reload : NavigationEvent
    }

    private val navigationEvents: MutableSharedFlow<NavigationEvent> = MutableSharedFlow()

    var title: String? by mutableStateOf(null)
        internal set
    var loadedUrl: String? by mutableStateOf(null)
        internal set
    var injectVConsole: Boolean by mutableStateOf(false)
        internal set
    var progress: Float by mutableFloatStateOf(0f)
        internal set

    @OptIn(FlowPreview::class)
    internal suspend fun handleNavigationEvents(
        onBack: () -> Unit = {},
        onForward: () -> Unit = {},
        reload: () -> Unit = {},
    ) = withContext(Dispatchers.Main) {
        navigationEvents.debounce(350).collect { event ->
            when (event) {
                NavigationEvent.Back -> onBack()
                NavigationEvent.Forward -> onForward()
                NavigationEvent.Reload -> reload()
            }
        }
    }

    fun navigateBack() {
        scope.launch { navigationEvents.emit(NavigationEvent.Back) }
    }

    fun navigateForward() {
        scope.launch { navigationEvents.emit(NavigationEvent.Forward) }
    }

    fun reload() {
        scope.launch { navigationEvents.emit(NavigationEvent.Reload) }
    }

    fun injectVConsole(): Boolean {
        injectVConsole = !injectVConsole
        reload()
        return injectVConsole
    }

}

@Composable
fun rememberWebViewNavigator(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewNavigator = remember(coroutineScope) { WebViewNavigator(coroutineScope) }
