package com.example.fragment.project.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.fragment.project.R
import com.example.miaow.base.utils.WebViewHelper
import com.example.miaow.base.utils.WebViewManager
import com.example.miaow.base.utils.injectVConsoleJs
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WebScreen(
    originalUrl: String,
    webCollectList: List<String>,
    onWebBrowse: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onWebCollect: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    val state = rememberWebViewState(originalUrl)
    val navigator = rememberWebViewNavigator()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    var injectVConsole by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {
        webView?.let {
            if (!WebViewHelper.goBack(it, originalUrl)) {
                onNavigateUp()
            }
        }
    }
    Column(
        modifier = Modifier
            .background(colorResource(R.color.white))
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            modifier = Modifier.weight(1f),
            sheetContent = {
                Row(
                    modifier = Modifier
                        .background(colorResource(R.color.white))
                        .height(50.dp)
                ) {
                    Button(
                        onClick = {
                            context.startActivity(Intent.createChooser(Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, state.lastLoadedUrl.toString())
                                type = "text/plain"
                            }, null))
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        contentPadding = PaddingValues(15.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_share),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            try {
                                if (context is AppCompatActivity) {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(state.lastLoadedUrl)
                                    )
                                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_browse),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            onWebCollect(
                                !webCollectList.contains(state.lastLoadedUrl.toString()),
                                state.lastLoadedUrl.toString()
                            )
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        contentPadding = PaddingValues(15.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(
                                if (webCollectList.contains(state.lastLoadedUrl.toString())) {
                                    R.mipmap.ic_collect_checked
                                } else {
                                    R.mipmap.ic_collect_unchecked
                                }
                            ),
                            contentDescription = null,
                            tint = colorResource(
                                if (webCollectList.contains(state.lastLoadedUrl.toString())) {
                                    R.color.pink
                                } else {
                                    R.color.theme
                                }
                            )
                        )
                    }
                    Button(
                        onClick = {
                            injectVConsole = !injectVConsole
                            navigator.reload()
                            scope.launch { sheetState.hide() }
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_debug),
                            contentDescription = null,
                            tint = colorResource(
                                if (injectVConsole) {
                                    R.color.theme_orange
                                } else {
                                    R.color.theme
                                }
                            )
                        )
                    }
                }
            }
        ) {
            var injectState by remember { mutableStateOf(false) }
            var progress by remember { mutableFloatStateOf(0f) }
            val client = object : AccompanistWebViewClient() {

                override fun doUpdateVisitedHistory(
                    view: WebView,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    if (!url.isNullOrBlank() && url != "about:blank") {
                        onWebBrowse(true, url.toString())
                    }
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    if (view != null && request != null) {
                        when {
                            WebViewHelper.isAssetsResource(request) -> {
                                return WebViewHelper.assetsResourceRequest(view.context, request)
                            }

                            WebViewHelper.isCacheResource(request) -> {
                                return WebViewHelper.cacheResourceRequest(view.context, request)
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    if (view != null && request != null && request.url != null) {
                        if ("http" != request.url.scheme && "https" != request.url.scheme) {
                            try {
                                view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return true
                        }
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

            val chromeClient = object : AccompanistWebChromeClient() {

                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progress = (newProgress / 100f).coerceIn(0f, 1f)
                    if (newProgress > 80 && injectVConsole && !injectState) {
                        view.apply { evaluateJavascript(context.injectVConsoleJs()) {} }
                        injectState = true
                    }
                }
            }
            WebView(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                captureBackPresses = false,
                navigator = navigator,
                onCreated = { webView ->
                    webView.settings.javaScriptEnabled = true
                    val forceDarkMode =
                        AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        webView.settings.isAlgorithmicDarkeningAllowed = forceDarkMode
                    } else {
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            WebSettingsCompat.setForceDark(
                                webView.settings,
                                if (forceDarkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                            )
                        }
                    }
                    WebViewHelper.setDownloadListener(webView)
                    WebViewHelper.setOnLongClickListener(webView)
                },
                onDispose = { WebViewManager.recycle(it) },
                client = client,
                chromeClient = chromeClient,
                factory = { context -> WebViewManager.obtain(context).also { webView = it } }
            )
            AnimatedVisibility(visible = (progress > 0f && progress < 1f)) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(R.color.theme_orange),
                    backgroundColor = colorResource(R.color.white)
                )
            }
        }
        Row(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .height(50.dp)
        ) {
            Button(
                onClick = {
                    webView?.let {
                        if (!WebViewHelper.goBack(it, originalUrl)) {
                            onNavigateUp()
                        }
                    }
                },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
                contentPadding = PaddingValues(17.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Icon(
                    painter = painterResource(R.mipmap.ic_web_back),
                    contentDescription = null,
                    tint = colorResource(R.color.theme)
                )
            }
            Button(
                onClick = {
                    if (navigator.canGoForward) {
                        navigator.navigateForward()
                    }
                },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
                contentPadding = PaddingValues(17.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Icon(
                    painter = painterResource(R.mipmap.ic_web_forward),
                    contentDescription = null,
                    tint = colorResource(R.color.theme)
                )
            }
            Button(
                onClick = { navigator.reload() },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
                contentPadding = PaddingValues(15.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Icon(
                    painter = painterResource(R.mipmap.ic_web_refresh),
                    contentDescription = null,
                    tint = colorResource(R.color.theme)
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        if (sheetState.isVisible) {
                            sheetState.hide()
                        } else {
                            sheetState.show()
                        }
                    }
                },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Icon(
                    painter = painterResource(R.mipmap.ic_web_more),
                    contentDescription = null,
                    tint = colorResource(R.color.theme)
                )
            }
        }
    }
}