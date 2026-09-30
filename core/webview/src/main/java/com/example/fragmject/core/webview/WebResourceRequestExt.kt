package com.example.fragmject.core.webview

import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest

private const val TAG = "WebResourceRequestExt"

/** 从 URL 提取文件扩展名（小写）。 */
internal fun WebResourceRequest.getExtensionFromUrl(): String {
    return try {
        MimeTypeMap.getFileExtensionFromUrl(url.toString())
    } catch (e: Exception) {
        Log.e(TAG, "getExtensionFromUrl failed: $url", e)
        ""
    }
}

/** 从 URL 推断 MIME 类型。 */
internal fun WebResourceRequest.getMimeTypeFromUrl(): String {
    return try {
        when (val extension = getExtensionFromUrl()) {
            "", "null", "*/*" -> "*/*"
            "json" -> "application/json"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        }
    } catch (e: Exception) {
        Log.e(TAG, "getMimeTypeFromUrl failed: $url", e)
        "*/*"
    }
}
