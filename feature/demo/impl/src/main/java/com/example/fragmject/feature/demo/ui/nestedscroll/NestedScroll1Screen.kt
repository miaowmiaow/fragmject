package com.example.fragmject.feature.demo.ui.nestedscroll

import android.annotation.SuppressLint
import android.content.Context
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
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.webview.WebViewAssetInterceptor
import org.json.JSONObject
import kotlin.math.abs

/** 量取 h5 页面实际高度用的脚本：返回文档宽高（px）。 */
private const val PAGE_SIZE_SCRIPT = """
    (function() {
        return {
            width: document.documentElement.scrollWidth || document.body.scrollWidth,
            height: document.documentElement.scrollHeight || document.body.scrollHeight
        };
    })()
"""

/** 底部占位色块，用于验证滚动嵌套衔接。 */
private val DEMO_BLOCK_COLORS = listOf(
    AppColors.yellow,
    AppColors.orange,
    AppColors.blue,
    AppColors.green,
    AppColors.pink,
    AppColors.alphaGray,
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NestedScroll1Screen() {
    val density = LocalDensity.current
    var boxHeight by remember { mutableIntStateOf(0) }
    val boxHeightDp = with(density) { boxHeight.toDp() }
    var columnHeight by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxHeight = it.height }
    ) {
        var webView by remember { mutableStateOf<WebView?>(null) }
        var webViewScrollHeight by remember { mutableIntStateOf(0) }
        val nestedScrollConnection = remember {
            WebViewNestedScrollConnection(
                webViewProvider = { webView },
                webViewScrollHeightProvider = { webViewScrollHeight },
                columnHeightProvider = { columnHeight },
                boxHeightProvider = { boxHeight },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .onSizeChanged { columnHeight = it.height }
        ) {
            AndroidView(
                factory = { context ->
                    createNestedScrollWebView(
                        context = context,
                        onCreated = { webView = it },
                        onScrollHeight = { webViewScrollHeight = it },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boxHeightDp),
                onRelease = { releaseWebView(it) }
            )
            DEMO_BLOCK_COLORS.forEach { color ->
                Box(
                    modifier = Modifier
                        .background(color)
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }
    }
}

/**
 * WebView 与父级 ScrollView 的滚动衔接。
 *
 * 滚动距离先交给 WebView，WebView 滚到底/顶后剩余距离交回父容器；
 * 累计滚动值按「WebView 高度 + Column 溢出高度」做边界限制。
 */
private class WebViewNestedScrollConnection(
    private val webViewProvider: () -> WebView?,
    private val webViewScrollHeightProvider: () -> Int,
    private val columnHeightProvider: () -> Int,
    private val boxHeightProvider: () -> Int,
) : NestedScrollConnection {

    private var scrollY = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val webViewScrollHeight = webViewScrollHeightProvider()
        val columnScrollHeight = columnHeightProvider() - boxHeightProvider()
        // 实际的滚动高度 = webView 的滚动高度 + column 的滚动高度
        val realScrollHeight = webViewScrollHeight + columnScrollHeight
        val delta = available.y
        scrollY += delta
        // 边界值限制
        scrollY = scrollY.coerceAtMost(0f).coerceAtLeast((-realScrollHeight).toFloat())
        val absScrollY = abs(scrollY).toInt()
        val webView = webViewProvider() ?: return Offset.Zero
        if (absScrollY >= webViewScrollHeight) {
            val dy = webView.scrollY - webViewScrollHeight
            // 滚动高度大于 webView 的滚动高度则交给父组件消费滚动距离
            if (dy >= 0) return Offset.Zero
            // 让 webView 滚动到底部并消费对应滚动距离
            webView.scrollTo(0, webViewScrollHeight)
            return Offset(0f, dy.toFloat())
        }
        if (webView.scrollY == webViewScrollHeight) {
            // 让 webView 滚动到对应滚动高度并消费对应滚动距离
            webView.scrollTo(0, absScrollY)
            return Offset(0f, (webViewScrollHeight - absScrollY).toFloat())
        }
        // 滚动高度小于 webView 的滚动高度则交给 webView 处理
        webView.scrollTo(0, absScrollY)
        return Offset(0f, delta)
    }
}

/** 创建演示用 WebView：启用 JS、拦截 assets 资源、页面加载完成后回传滚动高度。 */
@SuppressLint("SetJavaScriptEnabled")
private fun createNestedScrollWebView(
    context: Context,
    onCreated: (WebView) -> Unit,
    onScrollHeight: (Int) -> Unit,
): WebView = WebView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    settings.javaScriptEnabled = true
    webViewClient = HeightMeasuringWebViewClient(onScrollHeight)
    loadUrl("file:///android_asset/html/privacy_policy.html")
    onCreated(this)
}

/** 页面加载完成后量取 h5 高度，换算出 WebView 需要的滚动高度。 */
private class HeightMeasuringWebViewClient(
    private val onScrollHeight: (Int) -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (view != null && request != null && WebViewAssetInterceptor.isAssetsResource(request)) {
            return WebViewAssetInterceptor.assetsResourceRequest(view.context, request)
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        // 请确保能获取到 h5 的准确高度
        view.evaluateJavascript(PAGE_SIZE_SCRIPT.trimIndent()) { result ->
            // 页面尚未就绪时 evaluateJavascript 会回调 "null"，JSONObject 解析抛
            // JSONException；该回调在主线程且无 try/catch，未保护会直接崩溃。
            val realHeight = runCatching {
                val jsonObject = JSONObject(result)
                val width = jsonObject.getInt("width").toFloat()
                val height = jsonObject.getInt("height").toFloat()
                safeDivide(height, safeDivide(width, view.width.toFloat()))
            }.getOrNull() ?: return@evaluateJavascript
            onScrollHeight(convertOddToEven(realHeight - view.height))
        }
    }

    private fun safeDivide(numerator: Float, divisor: Float): Float =
        if (divisor != 0f) numerator / divisor else 0f

    private fun convertOddToEven(a: Float): Int = a.toInt() and -2
}

/** 退出时移除并销毁 WebView，避免泄漏。 */
private fun releaseWebView(webView: WebView) {
    if (webView.parent != null) {
        (webView.parent as ViewGroup).removeView(webView)
    }
    webView.removeAllViews()
    webView.destroy()
}
