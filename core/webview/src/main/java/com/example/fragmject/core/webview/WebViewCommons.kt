package com.example.fragmject.core.webview

import android.net.Uri
import android.webkit.WebView

/**
 * WebView 相关的通用判定，供业务模块复用。
 *
 * 目的：让 feature 层无需 import android.webkit.* 即可完成常见的
 * 「是否为 http(s) 链接」「长按命中是否为图片」判定，配合架构测试规则 13
 * 把 WebView 平台类型收敛在 core:webview 内。
 *
 * 这里刻意使用 android.net.Uri（core-ktx）而非 android.webkit.URLUtil 做链接判定：
 * URLUtil 属 webkit，且语义更宽（会接受 file/content 等协议）。
 */
object WebViewCommons {

    /** 是否为 http/https 链接（可直接用于下载或加载）。 */
    fun isHttpUrl(value: String): Boolean {
        val scheme = Uri.parse(value).scheme ?: return false
        return scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
    }

    /**
     * 长按命中结果是否为图片。
     *
     * 命中图片时 [WebView.HitTestResult.extra] 为图片地址，业务可据此弹出保存菜单。
     */
    fun isImageHitTest(result: WebView.HitTestResult): Boolean {
        return result.type == WebView.HitTestResult.IMAGE_TYPE ||
            result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
    }
}
