package com.example.fragmject.core.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.provider.MediaStore.MediaColumns.MIME_TYPE
import android.provider.MediaStore.MediaColumns.RELATIVE_PATH
import android.util.Log
import com.example.fragmject.core.common.utils.FileUtil
import com.example.fragmject.core.common.utils.getBitmapPathFromUri
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 纯媒体存储能力：把本地文件写入系统 MediaStore（图片/视频）。
 *
 * 不涉及网络下载，输入必须是已就绪的本地 [File]。由 [MediaRepositoryImpl]
 * 负责编排「下载 → 落盘 → 写相册」的完整流程。
 */
object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    suspend fun saveImage(context: Context, file: File): Pair<String, String> =
        save(
            context = context,
            file = file,
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            relativePath = Environment.DIRECTORY_PICTURES,
        )

    suspend fun saveVideo(context: Context, file: File): Pair<String, String> =
        save(
            context = context,
            file = file,
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            relativePath = Environment.DIRECTORY_MOVIES,
        )

    /**
     * 仅通知系统扫描已存在的媒体文件（不复制）。
     * 适用于文件已写入公共目录（如 Movies）后，让系统相册及时可见。
     */
    suspend fun notifyAdded(context: Context, file: File): Pair<String, String> =
        suspendCancellableCoroutine { cont ->
            try {
                val mimeType = FileUtil.getFileMimeType(file)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                ) { path, uri ->
                    if (cont.isActive) cont.resume((path ?: "") to (uri?.toString() ?: ""))
                }
            } catch (e: Exception) {
                Log.e(TAG, "notifyAdded failed: ${file.absolutePath}", e)
                if (cont.isActive) cont.resume("" to "")
            }
        }

    private suspend fun save(
        context: Context,
        file: File,
        collection: Uri,
        relativePath: String,
    ): Pair<String, String> = suspendCancellableCoroutine { cont ->
        var out: OutputStream? = null
        var fis: FileInputStream? = null
        try {
            val mimeType = FileUtil.getFileMimeType(file)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(DISPLAY_NAME, file.name)
                    put(MIME_TYPE, mimeType)
                    put(RELATIVE_PATH, relativePath)
                }
                val uri = context.contentResolver.insert(collection, values)
                if (uri == null) {
                    file.delete()
                    if (cont.isActive) cont.resume("" to "")
                    return@suspendCancellableCoroutine
                }
                out = context.contentResolver.openOutputStream(uri)
                if (out == null) {
                    file.delete()
                    if (cont.isActive) cont.resume("" to "")
                    return@suspendCancellableCoroutine
                }
                fis = FileInputStream(file)
                FileUtils.copy(fis, out)
                file.delete()
                if (cont.isActive) cont.resume(context.getBitmapPathFromUri(uri) to uri.toString())
            } else {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                ) { path, uri ->
                    file.delete()
                    if (cont.isActive) cont.resume((path ?: "") to (uri?.toString() ?: ""))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "save failed: ${file.absolutePath}", e)
            if (cont.isActive) cont.resume("" to "")
        } finally {
            FileUtil.quickClose(fis)
            FileUtil.quickClose(out)
        }
    }
}
