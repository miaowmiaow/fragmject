package com.example.fragment.project.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import com.google.accompanist.web.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(
    originalUrl: String
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    val state = rememberWebViewState(originalUrl)
    val navigator = rememberWebViewNavigator()
    //仅展示compose的权限申请，为写而写并没有实际意义
//    val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//        arrayOf(
//            Manifest.permission.READ_MEDIA_IMAGES,
//            Manifest.permission.READ_MEDIA_VIDEO,
//        )
//    else
//        arrayOf(
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//        )
//    val openDialog = remember {
//        mutableStateOf(!storagePermissions.all {
//            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        })
//    }
//    val requestPermissions =
//        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ps ->
//            var isGranted = true
//            ps.entries.forEach {
//                if (it.key in storagePermissions && !it.value)
//                    isGranted = false
//            }
//            openDialog.value = !isGranted
//        }
//    if (openDialog.value) {
//        AlertDialog(
//            onDismissRequest = { openDialog.value = false },
//            title = { Text(text = "申请存储空间权限") },
//            text = { Text(text = "玩Android需要使用存储空间，我们想要将文章内容缓存到本地，从而加快打开速度和减少用户流量使用") },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        requestPermissions.launch(storagePermissions)
//                    }
//                ) { Text("确定") }
//            },
//            dismissButton = {
//                TextButton(onClick = { openDialog.value = false }) { Text("取消") }
//            }
//        )
//    }
    Column(
        modifier = Modifier
            .background(colorResource(R.color.white))
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        var injectState by remember { mutableStateOf(false) }
        //注入VConsole以便于H5调试
        val injectVConsole by remember { mutableStateOf(false) }
        var progress by remember { mutableFloatStateOf(0f) }
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
        Divider(color = colorResource(R.color.line))
        Row(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .height(50.dp)
        ) {
            Button(
                onClick = {
                    webView?.let {
                        if (!WebViewHelper.goBack(it, originalUrl)) {
                            if (context is AppCompatActivity) {
                                context.onBackPressedDispatcher.onBackPressed()
                            }
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
                    painter = painterResource(R.drawable.ic_web_back),
                    contentDescription = null
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
                    painter = painterResource(R.drawable.ic_web_forward),
                    contentDescription = null
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
                contentPadding = PaddingValues(17.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_web_refresh),
                    contentDescription = null
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
                contentPadding = PaddingValues(17.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_web_browse),
                    contentDescription = null
                )
            }
        }
    }
}