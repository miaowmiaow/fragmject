package com.example.fragment.library.common.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.*
import com.example.fragment.library.common.R
import com.example.fragment.library.common.constant.Keys
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.accompanist.web.*

class WebFragment : RouterFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WanTheme {
                    WebScreen(Uri.decode(requireArguments().getString(Keys.URL)))
                }
            }
        }
    }

    override fun initView() {}

    override fun initViewModel(): BaseViewModel? {
        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun WebScreen(
        originalUrl: String
    ) {
        var webView by remember { mutableStateOf<WebView?>(null) }
        val state = rememberWebViewState(originalUrl)
        val navigator = rememberWebViewNavigator()
        val systemUiController = rememberSystemUiController()
        SideEffect {
            systemUiController.statusBarDarkContentEnabled = true
        }
        DisposableEffect(LocalLifecycleOwner.current) {
            onDispose {
                systemUiController.statusBarDarkContentEnabled = false
            }
        }
        Column(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            var injectState by remember { mutableStateOf(false) }
            //注入VConsole以便于H5调试
            val injectVConsole by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf(0f) }

            val client = object : AccompanistWebViewClient() {

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

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    injectState = false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectState = false
                }
            }

            val chromeClient = object : AccompanistWebChromeClient() {

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progress = (newProgress / 100f).coerceIn(0f, 1f)
                    if (newProgress > 80 && injectVConsole && !injectState) {
                        view?.apply { evaluateJavascript(context.injectVConsoleJs()) {} }
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
                    WebViewHelper.setDownloadListener(webView)
                    WebViewHelper.setOnLongClickListener(webView)
                },
                onDispose = { webView ->
                    WebViewManager.recycle(webView)
                },
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
            Divider(
                color = colorResource(R.color.line)
            )
            Row(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .height(50.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_web_back),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(17.dp)
                        .clickable {
                            webView?.let {
                                if (!WebViewHelper.goBack(it, originalUrl)) {
                                    onBackPressed()
                                }
                            }
                        }
                )
                Image(
                    painter = painterResource(R.drawable.ic_web_forward),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(17.dp)
                        .clickable {
                            if (navigator.canGoForward) {
                                navigator.navigateForward()
                            }
                        }
                )
                Image(
                    painter = painterResource(R.drawable.ic_web_refresh),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(17.dp)
                        .clickable {
                            navigator.reload()
                        }
                )
                Image(
                    painter = painterResource(R.drawable.ic_web_browse),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(17.dp)
                        .clickable {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(state.content.getCurrentUrl())
                                )
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                requireActivity().startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                )
            }
        }
    }
}