package com.example.fragmject.feature.authenticator.ui.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.fragmject.core.webview.WebViewContainer
import com.example.fragmject.core.webview.rememberWebViewControl
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 一个待导出的二维码条目。
 *
 * 放在 `ui.export` 而非 `components`：它不是可复用组件，
 * 只服务于导出展示页（[AuthenticatorExportQrScreen] 的数据契约）。
 */
data class QrCodeItem(
    /** 二维码上方的主标题（如发行方，或批量导出的「第 x/y 张」）。 */
    val caption: String,
    /** 主标题下的副标题（如账户名）；为空则不显示。 */
    val name: String = "",
    /** 二维码内容，通常是 `otpauth://` 或 `otpauth-migration://` URI。 */
    val content: String,
    /**
     * 渲染边长（CSS 像素）。
     *
     * 内容越长二维码版本越高、模块越密，同样边长下就越难扫，
     * 因此批量码要画得更大（[BATCH_QR_SIZE]）才能保证每个模块有足够像素。
     */
    val size: Int = DEFAULT_QR_SIZE,
) {
    companion object {
        /** 单个 `otpauth://` 内容短，240px 足够。 */
        const val DEFAULT_QR_SIZE = 240

        /** 批量码内容长（见 `OtpMigrationUri.MAX_PAYLOAD_CHARS`），需要更大画布。 */
        const val BATCH_QR_SIZE = 300
    }
}

private const val QR_PAGE_URL = "file:///android_asset/html/authenticator_qrcode.html"

/** 等待页面就绪的重渲染次数与间隔（总计约 3 秒）。 */
private const val RENDER_RETRY_TIMES = 20
private const val RENDER_RETRY_DELAY = 150L
private const val RENDER_CONFIRM_DELAY = 50L

/**
 * 导出二维码渲染：加载本模块 assets 中的离线 H5 页面，由原生注入数据渲染。
 *
 * 放在 `ui.export`（而非 `components`）：它硬编码了本模块的 H5 页面地址与
 * JSON 载荷格式，**只有一个使用方**（导出展示页），属于页面的一部分，不是可复用组件。
 *
 * **不引入任何二维码生成库**——编码由内联的 `assets/html/qrcode.min.js`（MIT）完成，
 * 渲染交给 WebView。页面受 CSP 约束（`default-src 'none'`）且不发起网络请求，
 * 因此 TOTP 密钥不会离开设备。
 *
 * **安全域混用（已知，待治理）**：本页复用 `core:webview` 的**池化** WebView，而池内实例是
 * 按「任意第三方网页」配置的（JS 开启、混合内容放行）。也就是说，承载 TOTP 密钥的本地页面与
 * 任意第三方文章页共用同一批实例。当前靠 CSP + 不发起网络请求缓解，但两者安全域本应隔离：
 * 治理方向是让本地敏感页走「最小权限」配置（关闭 JS、不进 keep-alive 池）。
 *
 * 渲染走两条互补路径：
 * - [scriptsProvider]：页面加载进度超过 80% 时注入一次，保证首次渲染发生在脚本就绪之后；
 * - `evaluateJavascript`：数据变化时主动重渲染（若页面尚未就绪则无效，由前者兜底）。
 *
 * **必须显式 reload**（真机踩坑）：`core:webview` 的 WebView 是池化复用的，
 * 且 `WebViewContainer` 只在 `this.url != url` 时才 `loadUrl`——第二次进入展示页时
 * url 未变、**页面根本不会重新加载**，「进度 >80% 注入一次」也就不会触发，
 * 于是页面一直显示上一次的二维码。因此每次数据变化都 reload 一次，
 * 强制走完整的「加载 → 注入」；再用轮询兜底，直到页面确认渲染成功。
 */
@Composable
fun QrCodeScreen(
    items: List<QrCodeItem>,
    modifier: Modifier = Modifier,
) {
    val control = rememberWebViewControl()
    val payload = remember(items) { encodePayload(items) }

    LaunchedEffect(payload) {
        control.reload()
        if (items.isEmpty()) return@LaunchedEffect
        // 兜底：renderQr 返回是否渲染成功，轮询到成功为止，避免注入早于页面就绪而被丢弃
        repeat(RENDER_RETRY_TIMES) {
            delay(RENDER_RETRY_DELAY.milliseconds)
            var rendered = false
            control.evaluateJavascript("renderQr($payload)") { result ->
                rendered = result.contains("true")
            }
            delay(RENDER_CONFIRM_DELAY.milliseconds)
            if (rendered) return@LaunchedEffect
        }
    }

    WebViewContainer(
        modifier = modifier,
        url = QR_PAGE_URL,
        control = control,
        scriptsProvider = { listOf("renderQr($payload)") },
    )
}

/**
 * 编码为 JSON 对象字面量，直接作为 JS 参数注入。
 *
 * 用 `org.json` 而非字符串拼接，确保账户名中的引号、换行、反斜杠被正确转义——
 * 手拼字符串一旦遇到特殊字符就会产出非法 JS，表现为「页面空白」这类难排查的问题。
 */
private fun encodePayload(items: List<QrCodeItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("caption", item.caption)
                .put("name", item.name)
                .put("content", item.content)
                .put("size", item.size),
        )
    }
    return JSONObject().put("items", array).toString()
}
