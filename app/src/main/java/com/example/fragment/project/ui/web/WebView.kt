package com.example.fragment.project.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.URLUtil
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
import com.example.miaow.base.utils.injectVConsoleJs
import com.example.miaow.base.utils.injectVideoJs
import com.example.miaow.base.utils.saveImagesToAlbum
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
    onReceivedTitle: (title: String?) -> Unit = {},
    onCustomView: (view: View?) -> Unit = {},
    shouldOverrideUrl: (url: String) -> Unit = {},
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var injectState by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var extra by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(webView, navigator) {
        webView?.let {
            with(navigator) {
                handleNavigationEvents(
                    reload = { it.reload() }
                )
            }
        }
    }
    val resourceToPermissionMap = mapOf(
        "android.webkit.resource.VIDEO_CAPTURE" to Manifest.permission.CAMERA,
        "android.webkit.resource.AUDIO_CAPTURE" to Manifest.permission.RECORD_AUDIO
    )
    var permissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val contract = ActivityResultContracts.RequestMultiplePermissions()
    val requestPermissions = rememberLauncherForActivityResult(contract) { result ->
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
            WebViewManager.obtain(context, url).apply {
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setDownloadListener { url, _, _, _, _ ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        intent.addCategory(Intent.CATEGORY_BROWSABLE)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(this.javaClass.name, e.message.toString())
                    }
                }
                setOnLongClickListener {
                    val result = hitTestResult
                    when (result.type) {
                        WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                            extra = result.extra
                            showDialog = true
                            true
                        }

                        else -> false
                    }
                }
                webChromeClient = object : WebChromeClient() {

                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        navigator.progress = (newProgress / 100f).coerceIn(0f, 1f)
                        if (newProgress > 80 && navigator.injectState && !injectState) {
                            evaluateJavascript(context.injectVideoJs()) {}
                            evaluateJavascript(context.injectVConsoleJs()) {}
                            injectState = true
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        onReceivedTitle(title)
                        view?.tag = title
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        super.onShowCustomView(view, callback)
                        onCustomView(view)
                    }

                    override fun onHideCustomView() {
                        super.onHideCustomView()
                        onCustomView(null)
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
                                WebViewManager.isCacheResource(request) -> {
                                    return WebViewManager.cacheResourceRequest(context, request)
                                }

                                WebViewManager.isAssetsResource(request) -> {
                                    return WebViewManager.assetsResourceRequest(context, request)
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
                tag?.let { title ->
                    onReceivedTitle(title.toString())
                }
                webView = this
            }
        },
        modifier = modifier,
        onRelease = {
            WebViewManager.recycle(it)
        }
    )
    val context = LocalContext.current
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

@Stable
class WebViewNavigator(private val scope: CoroutineScope) {
    private sealed interface NavigationEvent {
        data object Reload : NavigationEvent
    }

    private val navigationEvents: MutableSharedFlow<NavigationEvent> = MutableSharedFlow()

    var injectState: Boolean by mutableStateOf(false)
        internal set
    var progress: Float by mutableFloatStateOf(0f)
        internal set

    @OptIn(FlowPreview::class)
    internal suspend fun handleNavigationEvents(
        reload: () -> Unit = {},
    ) = withContext(Dispatchers.Main) {
        navigationEvents.debounce(350).collect { event ->
            when (event) {
                NavigationEvent.Reload -> reload()
            }
        }
    }

    fun reload() {
        scope.launch { navigationEvents.emit(NavigationEvent.Reload) }
    }

    fun inject(): Boolean {
        injectState = !injectState
        reload()
        return injectState
    }
}

@Composable
fun rememberWebViewNavigator(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewNavigator = remember(coroutineScope) { WebViewNavigator(coroutineScope) }
