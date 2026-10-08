package com.example.fragmject.core.android.platform

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri

/**
 * URI → 文件路径 转换工具。
 *
 * 从 [BitmapUtils] 中拆出，属于纯数据路径操作，不依赖 Bitmap/UI。
 * 供 data 层媒体存储（MediaStoreUtils）等模块直接引用此文件。
 */
fun Context.getBitmapPathFromUri(uri: Uri): String {
    var imagePath = ""
    if (DocumentsContract.isDocumentUri(this, uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        if ("com.android.providers.media.documents" == uri.authority) {
            val id = docId.split(":".toRegex()).toTypedArray()[1]
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection = MediaStore.Images.Media._ID + "=" + id
            imagePath = contentResolverQueryPath(contentUri, selection)
        } else if ("com.android.providers.downloads.documents" == uri.authority) {
            val uriString = "content://downloads/public_downloads"
            val contentUri = ContentUris.withAppendedId(uriString.toUri(), docId.toLong())
            imagePath = contentResolverQueryPath(contentUri)
        }
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        imagePath = contentResolverQueryPath(uri)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        imagePath = uri.path.toString()
    }
    return imagePath
}

fun Context.contentResolverQueryPath(uri: Uri, selection: String = ""): String {
    // 必须用 use：成功路径也曾直接 return，导致每次保存图片/视频都泄漏一个 Cursor
    val cursor = contentResolver.query(uri, null, selection, null, null) ?: return ""
    return cursor.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(MediaStore.Images.Media.DATA)
            if (index >= 0) it.getString(index) else ""
        } else {
            ""
        }
    }
}
