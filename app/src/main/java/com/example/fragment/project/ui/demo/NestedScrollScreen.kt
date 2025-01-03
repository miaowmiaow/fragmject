package com.example.fragment.project.ui.demo

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fragment.project.WanTheme
import com.example.fragment.project.ui.web.WebViewManager
import org.json.JSONObject
import kotlin.math.abs

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NestedScrollScreen() {
    Box {
        BoxWithConstraints {
            val density = LocalDensity.current
            val boxHeight = maxHeight
            val boxHeightPx = with(density) { boxHeight.toPx() }
            var columnHeight by remember { mutableFloatStateOf(0f) }
            var webView by remember { mutableStateOf<WebView?>(null) }
            var webViewScrollHeight by remember { mutableIntStateOf(0) }
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    var scrollY = 0f
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val otherScrollHeight = columnHeight - boxHeightPx
                        val realScrollHeight = webViewScrollHeight + otherScrollHeight
                        val delta = available.y
                        scrollY += delta
                        scrollY = scrollY.coerceAtMost(0f)
                        scrollY = scrollY.coerceAtLeast(-realScrollHeight)
                        val absScrollY = abs(scrollY).toInt()
                        webView?.let {
                            if (absScrollY < webViewScrollHeight) {
                                if (it.scrollY == webViewScrollHeight) {
                                    val offsetY = it.scrollY - absScrollY
                                    it.scrollTo(0, absScrollY)
                                    return Offset(0f, offsetY.toFloat())
                                }
                                it.scrollTo(0, absScrollY)
                                return Offset(0f, delta)
                            } else {
                                val offsetY = it.scrollY - webViewScrollHeight
                                if (offsetY < 0) {
                                    it.scrollTo(0, webViewScrollHeight)
                                    return Offset(0f, offsetY.toFloat())
                                }
                                return Offset.Zero
                            }
                        }
                        return Offset.Zero
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .onSizeChanged {
                        columnHeight = it.height.toFloat()
                    }
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            this.layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    if (view != null && request != null) {
                                        when {
                                            WebViewManager.isCacheResource(request) -> {
                                                return WebViewManager.cacheResourceRequest(
                                                    view.context,
                                                    request
                                                )
                                            }

                                            WebViewManager.isAssetsResource(request) -> {
                                                return WebViewManager.assetsResourceRequest(
                                                    view.context,
                                                    request
                                                )
                                            }
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    super.onPageFinished(view, url)
                                    view.evaluateJavascript(
                                        "(function() { return { width: document.body.scrollWidth, height: document.body.scrollHeight }; })();"
                                    ) { result ->
                                        val jsonObject = JSONObject(result)
                                        val width = jsonObject.getInt("width")
                                        val height = jsonObject.getInt("height")
                                        val screenWidth = view.width.toFloat()
                                        val screenHeight = view.height.toFloat()
                                        val scaleFactor = width / screenWidth
                                        webViewScrollHeight =
                                            (height / scaleFactor - screenHeight).toInt()
                                    }
                                }

                                fun convertOddToEven(a: Int): Int {
                                    return a and -2
                                }

                            }
                            loadUrl("file:///android_asset/privacy_policy.html")
                            webView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(boxHeight),
                    onRelease = {
                        if (it.parent != null) {
                            (it.parent as ViewGroup).removeView(it)
                        }
                        it.removeAllViews()
                        it.destroy()
                    }
                )
                Box(
                    modifier = Modifier
                        .background(WanTheme.yellow)
                        .fillMaxWidth()
                        .height(800.dp)
                ) { }
                Box(
                    modifier = Modifier
                        .background(WanTheme.orange)
                        .fillMaxWidth()
                        .height(800.dp)
                ) { }
            }
        }
    }
}