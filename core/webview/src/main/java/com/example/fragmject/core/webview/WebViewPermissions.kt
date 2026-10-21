package com.example.fragmject.core.webview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.PermissionRequest
import androidx.core.net.toUri

internal val WEB_PERMISSIONS = mapOf(
    PermissionRequest.RESOURCE_VIDEO_CAPTURE to Manifest.permission.CAMERA,
    PermissionRequest.RESOURCE_AUDIO_CAPTURE to Manifest.permission.RECORD_AUDIO,
)

internal val WEB_PERMISSION_LABELS = mapOf(
    PermissionRequest.RESOURCE_VIDEO_CAPTURE to "摄像头",
    PermissionRequest.RESOURCE_AUDIO_CAPTURE to "麦克风",
)

internal fun PermissionRequest.supportedWebResources(): List<String> =
    resources.filter { it in WEB_PERMISSIONS }

/** 仅 https 且带主机名的来源可申请敏感权限；http、file 及无来源一律拒绝。 */
internal fun PermissionRequest.hasTrustedOrigin(): Boolean {
    val origin = origin ?: return false
    return origin.scheme.equals("https", ignoreCase = true) && !origin.host.isNullOrBlank()
}

internal fun PermissionRequest.answerGrant(resources: List<String>) {
    try {
        grant(resources.toTypedArray())
    } catch (e: Exception) {
        Log.w("WebView", "grant permission failed", e)
    }
}

internal fun PermissionRequest.answerDeny() {
    try {
        deny()
    } catch (e: Exception) {
        Log.w("WebView", "deny permission failed", e)
    }
}


private val ALLOWED_DOWNLOAD_SCHEMES = setOf("http", "https")

internal fun handleDownload(context: Context, url: String) {
    val uri = url.toUri()
    // URL 来自页面的 DownloadListener，可能是 file:// / content:// / 自定义 scheme；
    // 直接 ACTION_VIEW 会把本地文件或应用私有内容交给外部 App，故只放行 http(s)。
    if (uri.scheme?.lowercase() !in ALLOWED_DOWNLOAD_SCHEMES) {
        Log.w("WebView", "拒绝非 http(s) 下载 URL：$url")
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            // 回调不一定发生在 Activity 上下文，显式 NEW_TASK 避免抛异常
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        Log.e("WebView", "无应用可处理下载链接：$url", e)
    } catch (e: Exception) {
        Log.e("WebView", "setOnDownloadListener: open url failed: $url", e)
    }
}

/**
 * 通用 WebView 控制：reload / evaluateJavascript / 加载进度。
 *
 * 不含任何业务语义；文章调试脚本开关等由业务层自行管理。
 */
