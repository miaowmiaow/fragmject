package com.example.fragmject.feature.article.ui.web

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.fragmject.core.ui.components.StandardDialog

@Composable
fun rememberWebMediaController(): WebMediaController =
    remember { WebMediaController() }

/**
 * 渲染 WebView 媒体交互相关的对话框与 Toast。
 *
 * 实际图片保存编排通过 [onSaveImage] 回调交给 ViewModel，
 * 本组件只负责对话框状态、旧版本存储权限申请与 Toast 反馈，保持纯 UI 职责。
 */
@Composable
fun WebMediaDialogs(
    controller: WebMediaController,
    onSaveImage: (extra: String?, onResult: (Boolean) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    // 等待存储权限授权的图片 URI（仅 API ≤28 会用到）
    var pendingExtra by remember { mutableStateOf<String?>(null) }

    val save = { extra: String ->
        onSaveImage(extra) { success ->
            Toast.makeText(
                context,
                if (success) "保存图片成功" else "保存图片失败",
                Toast.LENGTH_SHORT
            ).show()
            controller.dismissImageDialog()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val extra = pendingExtra
        pendingExtra = null
        if (granted && extra != null) {
            save(extra)
        } else {
            Toast.makeText(context, "保存失败：未授予存储权限", Toast.LENGTH_SHORT).show()
            controller.dismissImageDialog()
        }
    }

    // 图片保存确认
    StandardDialog(
        show = controller.showImageDialog,
        title = "提示",
        text = "你希望保存该图片吗？",
        onConfirm = {
            val extra = controller.imageExtra
            if (extra != null) {
                if (needsLegacyStoragePermission(context)) {
                    pendingExtra = extra
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    save(extra)
                }
            }
        },
        onDismiss = { controller.dismissImageDialog() },
    )
}

/**
 * API ≤28 把图片复制到公共相册需要 `WRITE_EXTERNAL_STORAGE`（manifest 上 `maxSdkVersion=28`）；
 * Android 10 起走 MediaStore，不再需要该权限。
 */
@Suppress("DEPRECATION")
private fun needsLegacyStoragePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) != PackageManager.PERMISSION_GRANTED
