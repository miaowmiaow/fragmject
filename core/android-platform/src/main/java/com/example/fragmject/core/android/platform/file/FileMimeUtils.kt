package com.example.fragmject.core.android.platform.file

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLConnection
import java.nio.file.Files
import java.util.Locale
import com.example.fragmject.core.android.platform.log.LogSanitizer

/**
 * FileUtils 拆分文件之三：MIME 推断 / 文件头解析。
 */
private const val TAG = "FileUtils"

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

/** 文件头魔数 → MIME 的有序映射；顺序即优先级，首个前缀命中即返回。 */
private val HEAD_CODE_MIME_TYPES: List<Pair<String, String>> = listOf(
    "FFD8FF" to "image/jpeg",
    "89504E" to "image/png",
    "474946" to "image/gif",
    "524946" to "image/webp",
    "49492A00" to "image/tiff",
    "424D" to "image/bmp",
    "3C3F786D6C" to "application/xml",
    "68746D6C3E" to "text/html",
    "255044462D312E" to "application/pdf",
    "504B0304" to "application/zip",
    "52617221" to "application/rar",
    "57415645" to "audio/x-wav",
    "41564920" to "video/x-msvideo",
    "2E524D46" to "application/vnd.rn-realmedia",
    "000001B" to "video/mpeg",
)

internal fun parseHeadCodeInternal(headCode: String): String {
    val head = headCode.uppercase(Locale.getDefault())
    return HEAD_CODE_MIME_TYPES.firstOrNull { (magic, _) -> head.startsWith(magic) }?.second ?: "*/*"
}

internal fun readFileHeadStringInternal(file: File): String {
    val inputStream = try {
        FileInputStream(file)
    } catch (e: Exception) {
        Log.e(TAG, "readFileHeadString open failed: ${LogSanitizer.path(file.absolutePath)}", e)
        null
    }
    val bytes = readStreamBytesInternal(inputStream, 16)
    return bytesToHexString(bytes)
}

internal fun readFileHeadStringInternal(context: Context, fileUri: Uri): String {
    val inputStream = try {
        context.contentResolver.openInputStream(fileUri)
    } catch (e: Exception) {
        Log.e(TAG, "readFileHeadString open failed: ${LogSanitizer.url(fileUri.toString())}", e)
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
        Log.e(TAG, "getFileMimeType(uri) MediaMetadataRetriever failed: ${LogSanitizer.url(fileUri.toString())}", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    val resolver = context.contentResolver
    try {
        mimeType = resolver.getType(fileUri)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType(uri) contentResolver.getType failed: ${LogSanitizer.url(fileUri.toString())}", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    try {
        mimeType = parseHeadCodeInternal(readFileHeadStringInternal(context, fileUri))
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType(uri) parseHeadCode failed: ${LogSanitizer.url(fileUri.toString())}", e)
    }
    return mimeType ?: "*/*"
}

internal fun getFileMimeTypeInternal(file: File): String {
    if (!file.isFile) {
        return "*/*"
    }
    var mimeType: String? = null
    val filePath = file.absolutePath
    // 文件头优先于扩展名：图片下载后常以无扩展名的临时名交接（如 `image_<timestamp>`），
    // 扩展名推断必然落空，随后 MediaMetadataRetriever 对图片只会抛
    // "setDataSource failed"（它面向音视频），既拿不到结果又刷一条错误日志。
    // 魔数能确定媒体类型时直接返回，剩余链路只为非媒体文件保留。
    val headMimeType = runCatching { getFileTypeCodeInternal(file) }.getOrDefault("*/*")
    if (headMimeType.startsWith("image/") ||
        headMimeType.startsWith("audio/") ||
        headMimeType.startsWith("video/")
    ) {
        return headMimeType
    }
    // 用 java.net.URLConnection 而非 android.webkit.MimeTypeMap：后者属 WebView 包，
    // 会让平台层莫名依赖 webkit（架构规则要求 webkit 只出现在 core:webview）。
    try {
        mimeType = URLConnection.guessContentTypeFromName(filePath)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType guessContentType failed: ${LogSanitizer.path(filePath)}", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    val mmr = MediaMetadataRetriever()
    try {
        mmr.setDataSource(filePath)
        mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
    } catch (e: Exception) {
        Log.e(TAG, "getFileMimeType MediaMetadataRetriever failed: ${LogSanitizer.path(filePath)}", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    try {
        mimeType = Files.probeContentType(file.toPath())
    } catch (e: IOException) {
        Log.e(TAG, "getFileMimeType probeContentType failed: ${LogSanitizer.path(filePath)}", e)
    }
    if (mimeType != null) {
        return mimeType
    }
    return headMimeType
}
