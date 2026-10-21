package com.example.fragmject.feature.article.ui.web

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * WebView 媒体交互状态机。
 *
 * 承接原先内聚在 WebView 内的「图片保存」UI 状态：
 * - [onLongPressImage] 由 WebView 长按图片回调触发；
 * 对话框渲染与 Toast 由 [WebMediaDialogs] 负责，本类只维护状态，便于独立测试。
 */
@Stable
class WebMediaController {
    var showImageDialog by mutableStateOf(false)
        private set
    var imageExtra by mutableStateOf<String?>(null)
        private set

    fun onLongPressImage(extra: String?) {
        imageExtra = extra
        showImageDialog = true
    }

    fun dismissImageDialog() {
        showImageDialog = false
    }
}
