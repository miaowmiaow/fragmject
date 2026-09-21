package com.example.fragmject.core.ui.utils

import android.text.Html

/**
 * HTML → 纯文本（UI 展示专用）。
 *
 * 原位于 article 业务 UI 层的私有函数，现提取为通用工具，
 * 供各 feature 模块在业务映射层（Article → FeedCardUiState）复用，避免重复。
 */
fun fromHtml(str: String): String {
    return Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
}
