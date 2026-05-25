package com.example.miaow.base.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.util.Locale

/**
 * FileUtil 拆分文件之三：MIME 推断 / 文件头解析。
 */
private const val TAG = "FileUtil"

private fun bytesToHexString(src: ByteArray?): String {
    val sb = StringBuilder()
    if (src != null && src.isNotEmpty()) {
        for (b in src) {
            val v = b.toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                sb.append(0)
            }
            sb.append(hv)
        }
    }
    return sb.toString()
}

internal fun parseHeadCodeInternal(headCode: String): String {
    val head = headCode.uppercase(Locale.getDefault())
    return when {
        head.startsWith("FFD8FF") -> "image/jpeg"
        head.startsWith("89504E") -> "image/png"
        head.startsWith("474946") -> "image/gif"
        head.startsWith("524946") -> "image/webp"
        head.startsWith("49492A00") -> "image/tiff"
        head.startsWith("424D") -> "image/bmp"
        head.startsWith("3C3F786D6C") -> "application/xml"
        head.startsWith("68746D6C3E") -> "text/html"
        head.startsWith("255044462D312E") -> "application/pdf"
        head.startsWith("504B0304") -> "application/zip"
        head.startsWith("52617221") -> "application/rar"
        head.startsWith("57415645") -> "audio/x-wav"
        head.startsWith("41564920") -> "video/x-msvideo"
        head.startsWith("2E524D46") -> "application/vnd.rn-realmedia"
        head.startsWith("000001B") -> "video/mpeg"
        else -> "*/*"
    }
}

internal fun readFileHeadStringInternal(file: File): String {
    val inputStream = try {
        FileInputStream(file)
    } catch (e: Exception) {
        Log.e(TAG, "readFileHeadString open failed: ${file.absolutePath}", e)
        null
    }
    val bytes = readStreamBytesInternal(inputStream, 16)
    return bytesToHexString(bytes)
}

internal fun readFileHeadStringInternal(context: Context, fileUri: Uri): String {
    val inputStream = try {
        context.contentResolver.openInputStream(fileUri)
    } catch (e: Exception) {
        Log.e(TAG, "readFileHeadString open failed: $fileUri", e)
        null
    }
    val bytes = readStreamBytesInternal(inputStream, 16)
    return bytesToHexString(bytes)
}

internal fun getFileTypeCodeInternal(file: File): String {
    return parseHeadCodeInternal(readFileHeadStringInternal(file))
}

internal fun getFileTypeCodeInternal(context: Context, fileUri: Uri): String {
    return parseHeadCodeInternal(readFileHeadStringInternal(context, fileUri))
}

internal fun getFileMimeTypeInternal(context: Context, fileUri: Uri): String {
    var mimeType: String? = null
    val mmr = MediaMetadataRetriever()
    try {
        mmr.setDataSource(context, fileUri)
        mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType(uri) MediaMetadataRetriever failed: $fileUri", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    val resolver = context.contentResolver
    try {
        mimeType = resolver.getType(fileUri)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType(uri) contentResolver.getType failed: $fileUri", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    try {
        mimeType = parseHeadCodeInternal(readFileHeadStringInternal(context, fileUri))
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType(uri) parseHeadCode failed: $fileUri", e)
    }
    return mimeType ?: "*/*"
}

internal fun getFileMimeTypeInternal(file: File): String {
    if (!file.isFile) {
        return "*/*"
    }
    var mimeType: String? = null
    val filePath = file.absolutePath
    val suffix = MimeTypeMap.getFileExtensionFromUrl(filePath)
    try {
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType MimeTypeMap failed: $filePath", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    val mmr = MediaMetadataRetriever()
    try {
        mmr.setDataSource(filePath)
        mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType MediaMetadataRetriever failed: $filePath", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    try {
        mimeType = Files.probeContentType(file.toPath())
    } catch (e: IOException) {
        Log.e(TAG, "getFileMimeType probeContentType failed: $filePath", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    try {
        mimeType = getFileTypeCodeInternal(file)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType getFileTypeCode failed: $filePath", e)
    }
    return mimeType ?: "*/*"
}
