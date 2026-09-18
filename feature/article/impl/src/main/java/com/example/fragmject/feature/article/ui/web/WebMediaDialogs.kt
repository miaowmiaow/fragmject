package com.example.fragmject.feature.article.ui.web

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.fragmject.core.ui.components.StandardDialog

/**
 * WebView 媒体交互状态机。
 *
 * 承接原先内聚在 WebView 内的「图片保存 / 视频下载」UI 状态：
 * - [onLongPressImage] 由 WebView 长按图片回调触发；
 * - [onVideoDetected] 由 JS 桥接回调触发（空列表 = 无视频，单条 = 确认下载，多条 = 选择）。
 * 对话框渲染与 Toast 由 [WebMediaDialogs] 负责，本类只维护状态，便于独立测试。
 */
@Stable
class WebMediaController {
    var showImageDialog by mutableStateOf(false)
        private set
    var imageExtra by mutableStateOf<String?>(null)
        private set
    var showVideoDialog by mutableStateOf(false)
        private set
    var videoSaveUrl by mutableStateOf<String?>(null)
        private set
    var videoUrlList by mutableStateOf<List<String>>(emptyList())
        private set
    var showVideoSelectDialog by mutableStateOf(false)
        private set
    var noVideoFound by mutableStateOf(false)
        private set

    fun onLongPressImage(extra: String?) {
        imageExtra = extra
        showImageDialog = true
    }

    fun onVideoDetected(urls: List<String>) {
        when {
            urls.isEmpty() -> noVideoFound = true
            urls.size == 1 -> {
                videoSaveUrl = urls[0]
                showVideoDialog = true
            }

            else -> {
                videoUrlList = urls
                showVideoSelectDialog = true
            }
        }
    }

    fun dismissImageDialog() {
        showImageDialog = false
    }

    fun dismissVideoDialog() {
        showVideoDialog = false
    }

    fun dismissVideoSelectDialog() {
        showVideoSelectDialog = false
    }

    fun selectVideo(url: String) {
        videoSaveUrl = url
        showVideoSelectDialog = false
        showVideoDialog = true
    }

    fun consumeNoVideoFound() {
        noVideoFound = false
    }
}

@Composable
fun rememberWebMediaController(): WebMediaController =
    remember { WebMediaController() }

/**
 * 渲染 WebView 媒体交互相关的对话框与 Toast。
 *
 * 实际图片保存 / 视频下载编排通过 [onSaveImage] / [onRegisterVideo] 回调交给 ViewModel，
 * 本组件只负责对话框状态与 Toast 反馈，保持纯 UI 职责。
 */
@Composable
fun WebMediaDialogs(
    controller: WebMediaController,
    title: String?,
    onSaveImage: (extra: String?, onResult: (Boolean) -> Unit) -> Unit,
    onRegisterVideo: (title: String?, url: String) -> Unit,
) {
    val context = LocalContext.current

    // 图片保存确认
    StandardDialog(
        show = controller.showImageDialog,
        title = "提示",
        text = "你希望保存该图片吗？",
        onConfirm = {
            val extra = controller.imageExtra
            if (extra != null) {
                onSaveImage(extra) { success ->
                    Toast.makeText(
                        context,
                        if (success) "保存图片成功" else "保存图片失败",
                        Toast.LENGTH_SHORT
                    ).show()
                    controller.dismissImageDialog()
                }
            }
        },
        onDismiss = { controller.dismissImageDialog() },
    )

    // 视频保存确认
    StandardDialog(
        show = controller.showVideoDialog,
        title = "提示",
        text = controller.videoSaveUrl?.let { url ->
            val ext = url.substringAfterLast(".").substringBefore("?").lowercase()
            if (ext == "m3u8") "检测到 m3u8 流媒体视频，下载后合并为 .ts 文件（可在大部分播放器播放）。是否继续？"
            else "你希望保存该视频吗？"
        } ?: "你希望保存该视频吗？",
        onConfirm = {
            val url = controller.videoSaveUrl ?: return@StandardDialog
            controller.dismissVideoDialog()
            onRegisterVideo(title, url)
            Toast.makeText(context, "已开始下载", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { controller.dismissVideoDialog() },
    )

    // 未检测到视频 Toast 提示
    LaunchedEffect(controller.noVideoFound) {
        if (controller.noVideoFound) {
            Toast.makeText(context, "页面中未检测到视频", Toast.LENGTH_SHORT).show()
            controller.consumeNoVideoFound()
        }
    }

    // 多视频选择对话框
    if (controller.showVideoSelectDialog) {
        AlertDialog(
            onDismissRequest = { controller.dismissVideoSelectDialog() },
            title = { Text("选择要下载的视频") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    controller.videoUrlList.forEachIndexed { index, url ->
                        TextButton(
                            onClick = { controller.selectVideo(url) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${index + 1}. ${
                                    url.substringAfterLast("/").substringBefore("?")
                                }",
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { controller.dismissVideoSelectDialog() }) {
                    Text("取消", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
