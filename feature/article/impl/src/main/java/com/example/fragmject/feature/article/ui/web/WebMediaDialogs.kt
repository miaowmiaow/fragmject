package com.example.fragmject.feature.article.ui.web

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.fragmject.core.ui.components.StandardDialog

@Composable
fun rememberWebMediaController(): WebMediaController =
    remember { WebMediaController() }

/**
 * 渲染 WebView 媒体交互相关的对话框与 Toast。
 *
 * 实际图片保存编排通过 [onSaveImage] 回调交给 ViewModel，
 * 本组件只负责对话框状态与 Toast 反馈，保持纯 UI 职责。
 */
@Composable
fun WebMediaDialogs(
    controller: WebMediaController,
    onSaveImage: (extra: String?, onResult: (Boolean) -> Unit) -> Unit,
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
}