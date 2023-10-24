package com.example.fragment.project.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
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
    originalUrl: String,
    navigator: WebViewNavigator,
    modifier: Modifier = Modifier,
    goBack: () -> Unit = {},
    goForward: () -> Unit = {},
    shouldOverrideUrl: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val url by remember { mutableStateOf(originalUrl) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var injectState by remember { mutableStateOf(false) }
    BackHandler(true) {
        navigator.navigateBack()
    }
    webView?.let {
        LaunchedEffect(it, navigator) {
            with(navigator) {
                handleNavigationEvents(
                    onBack = {
                        if (WebViewManager.back(it)) {
                            goBack()
                        } else {
                            onNavigateUp()
                        }
                    },
                    onForward = {
                        if (WebViewManager.forward(it)) {
                            goForward()
                        }
                    },
                    reload = {
                        it.reload()
                    }
                )
            }
        }
    }
    AndroidView(
        factory = { context ->
            WebViewManager.obtain(context, url).apply {
                setDownloadListener()
                setOnLongClickListener()
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val forceDarkMode =
                    AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    settings.isAlgorithmicDarkeningAllowed = forceDarkMode
                } else {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        WebSettingsCompat.setForceDark(
                            settings,
                            if (forceDarkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                        )
                    }
                }
                webChromeClient = object : WebChromeClient() {

                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        navigator.progress = (newProgress / 100f).coerceIn(0f, 1f)
                        if (newProgress > 80 && navigator.injectVConsole && !injectState) {
                            view.apply { evaluateJavascript(context.injectVConsoleJs()) {} }
                            injectState = true
                        }
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
                        if (!URLUtil.isValidUrl(requestUrl)) {
                            try {
                                view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                            } catch (e: Exception) {
                                Log.e(this.javaClass.name, e.message.toString())
                                onNavigateUp()
                            }
                            return true
                        }
                        if (!request.isRedirect && URLUtil.isNetworkUrl(requestUrl) && requestUrl != url) {
                            shouldOverrideUrl(requestUrl)
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        navigator.lastLoadedUrl = url
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
    private val coroutineScope: CoroutineScope
) {
    private sealed interface NavigationEvent {
        data object Back : NavigationEvent
        data object Forward : NavigationEvent
        data object Reload : NavigationEvent
    }

    private val navigationEvents: MutableSharedFlow<NavigationEvent> = MutableSharedFlow()

    var lastLoadedUrl: String? by mutableStateOf(null)
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
                is NavigationEvent.Back -> onBack()
                is NavigationEvent.Forward -> onForward()
                is NavigationEvent.Reload -> reload()
            }
        }
    }

    fun navigateBack() {
        coroutineScope.launch { navigationEvents.emit(NavigationEvent.Back) }
    }

    fun navigateForward() {
        coroutineScope.launch { navigationEvents.emit(NavigationEvent.Forward) }
    }

    fun reload() {
        coroutineScope.launch { navigationEvents.emit(NavigationEvent.Reload) }
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
