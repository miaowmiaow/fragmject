package com.example.fragmject.core.webview

import android.webkit.WebView

/**
 * WebView 相关的通用判定，供业务模块复用。
 *
 * 目的：让 feature 层通过纯文本判断「是否为 http(s) 链接」，
 * 长按图片命中判定仅供模块内部使用，业务通过 WebViewContainer 的图片事件接收地址，
 * 配合架构测试规则 13 把 WebView 平台类型收敛在 core:webview 内。
 *
 * 链接判定刻意用纯字符串前缀比较：既不依赖 android.webkit.URLUtil（语义过宽，
 * 会接受 javascript:/content:/file: 等协议），也便于 JVM 单测直接覆盖。
 */
object WebViewCommons {

    private const val ASSET_PREFIX = "file:///android_asset/"

    /** 是否为 http/https 链接（可直接用于下载或加载）。 */
    fun isHttpUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)

    /**
     * 是否允许由容器作为页面主动加载：http(s)，或应用内置 assets 资源。
     *
     * 其余协议（javascript:、content:、其他 file: 路径、自定义 scheme 等）一律拒绝；
     * assets 路径禁止出现 `..` 及其百分号编码，避免越出资源目录。
     */
    fun isLoadableUrl(value: String): Boolean = isHttpUrl(value) || isBundledAssetUrl(value)

    private fun isBundledAssetUrl(value: String): Boolean {
        if (!value.startsWith(ASSET_PREFIX)) return false
        val path = value.substring(ASSET_PREFIX.length)
        return path.isNotEmpty() && !path.contains("..") && !path.contains("%2e", ignoreCase = true)
    }

    /**
     * 长按命中结果是否为图片。
     *
     * 命中图片时 [WebView.HitTestResult.extra] 为图片地址，业务可据此弹出保存菜单。
     */
    internal fun isImageHitTest(result: WebView.HitTestResult): Boolean {
        return result.type == WebView.HitTestResult.IMAGE_TYPE ||
            result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
    }
}
