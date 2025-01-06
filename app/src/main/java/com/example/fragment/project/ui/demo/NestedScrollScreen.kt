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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val density = LocalDensity.current
    var boxHeight by remember { mutableIntStateOf(0) }
    val boxHeightDp = with(density) { boxHeight.toDp() }
    var columnHeight by remember { mutableIntStateOf(0) }
    Box(modifier = Modifier
        .fillMaxSize()
        .onSizeChanged {
            boxHeight = it.height
        }) {
        var webView by remember { mutableStateOf<WebView?>(null) }
        var webViewScrollHeight by remember { mutableIntStateOf(0) }
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                var scrollY = 0f
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val columnScrollHeight = columnHeight - boxHeight
                    //实际的滚动高度 = webView的滚动高度 + column的滚动高度
                    val realScrollHeight = webViewScrollHeight + columnScrollHeight
                    val delta = available.y
                    scrollY += delta
                    //边界值限制
                    scrollY = scrollY.coerceAtMost(0f)
                    scrollY = scrollY.coerceAtLeast((-realScrollHeight).toFloat())
                    val absScrollY = abs(scrollY).toInt()
                    webView?.let {
                        if (absScrollY < webViewScrollHeight) {
                            if (it.scrollY == webViewScrollHeight) {
                                //滚动高度小于webView的滚动高度时，让webView滚动到对应滚动高度并消费对应滚动距离
                                it.scrollTo(0, absScrollY)
                                return Offset(0f, (webViewScrollHeight - absScrollY).toFloat())
                            }
                            it.scrollTo(0, absScrollY)//滚动高度小于webView的滚动高度则交给webView处理
                            return Offset(0f, delta)//webView消费滚动距离
                        } else {
                            val dy = it.scrollY - webViewScrollHeight
                            //滚动高度大于webView的滚动高度时，让webView滚动到底部并消费对应滚动距离
                            if (dy < 0) {
                                it.scrollTo(0, webViewScrollHeight)
                                return Offset(0f, dy.toFloat())
                            }
                            return Offset.Zero//滚动高度大于webView的滚动高度则交给父组件消费滚动距离
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
                    columnHeight = it.height
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
                                //请确保能获取到h5的准确高度
                                view.evaluateJavascript(
                                    """
                                        (function() {
                                            return {
                                                width: document.documentElement.scrollWidth || document.body.scrollWidth,
                                                height: document.documentElement.scrollHeight || document.body.scrollHeight
                                            };
                                        })()
                                    """.trimIndent()
                                ) { result ->
                                    val jsonObject = JSONObject(result)
                                    val width = jsonObject.getInt("width").toFloat()
                                    val height = jsonObject.getInt("height").toFloat()
                                    val scaleFactor = safeDivide(width, view.width.toFloat())
                                    val realHeight = safeDivide(height, scaleFactor)
                                    webViewScrollHeight = convertOddToEven(realHeight - view.height)
                                }
                            }

                            fun safeDivide(numerator: Float, divisor: Float): Float {
                                return if (divisor != 0f) numerator / divisor else 0f
                            }

                            fun convertOddToEven(a: Float): Int {
                                return a.toInt() and -2
                            }
                        }
                        loadUrl("file:///android_asset/privacy_policy.html")
                        webView = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boxHeightDp),
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
                    .height(300.dp)
            ) { }
            Box(
                modifier = Modifier
                    .background(WanTheme.orange)
                    .fillMaxWidth()
                    .height(300.dp)
            ) { }
            Box(
                modifier = Modifier
                    .background(WanTheme.blue)
                    .fillMaxWidth()
                    .height(300.dp)
            ) { }
            Box(
                modifier = Modifier
                    .background(WanTheme.green)
                    .fillMaxWidth()
                    .height(300.dp)
            ) { }
            Box(
                modifier = Modifier
                    .background(WanTheme.pink)
                    .fillMaxWidth()
                    .height(300.dp)
            ) { }
            Box(
                modifier = Modifier
                    .background(WanTheme.alphaGray)
                    .fillMaxWidth()
                    .height(300.dp)
            ) { }
        }
    }
}