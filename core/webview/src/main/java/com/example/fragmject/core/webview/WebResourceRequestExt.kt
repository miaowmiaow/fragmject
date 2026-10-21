package com.example.fragmject.core.webview

import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest

/**
 * 从 URL 提取文件扩展名（小写）。
 *
 * 不使用 try/catch：`MimeTypeMap.getFileExtensionFromUrl` 对非法 URL 返回空串、不抛异常，
 * 原先的兜底 catch 是死代码，只会让异常面看起来比实际更宽。
 */
internal fun WebResourceRequest.getExtensionFromUrl(): String =
    MimeTypeMap.getFileExtensionFromUrl(url.toString())

/** 从 URL 推断 MIME 类型；无法识别时统一回落为通配类型（星号斜杠星号）。 */
internal fun WebResourceRequest.getMimeTypeFromUrl(): String =
    when (val extension = getExtensionFromUrl()) {
        "", "null", "*/*" -> "*/*"
        "json" -> "application/json"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
