package com.example.fragmject.core.android.platform

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils as OsFileUtils
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.provider.MediaStore.MediaColumns.MIME_TYPE
import android.provider.MediaStore.MediaColumns.RELATIVE_PATH
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 纯媒体存储能力：把本地文件写入系统 MediaStore（图片/视频），
 * 以及为拍照预创建/清理 MediaStore Uri 的 pending 机制。
 *
 * 不涉及网络下载，输入必须是已就绪的本地 [File]。由 data 层的
 * MediaRepository 实现负责编排「下载 → 落盘 → 写相册」的完整流程。
 */
object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"

    /**
     * 查询系统相册图片的原始行数据，过滤 pending 空记录（如拍照预创建）。
     * 与 [saveImage] 等写入方法对称：均为对 MediaStore 的平台级访问原语。
     */
    suspend fun queryImages(context: Context): List<MediaRow> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.HEIGHT,
            MediaStore.Images.ImageColumns.MIME_TYPE,
            MediaStore.Images.ImageColumns.SIZE,
            MediaStore.Images.ImageColumns.WIDTH,
        )
        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_MODIFIED} DESC"

        val rows = ArrayList<MediaRow>()
        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)
            ?: return@withContext rows
        cursor.use {
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID)
            val bucketCol =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            val dateModifiedCol =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_MODIFIED)
            val displayNameCol =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.HEIGHT)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.WIDTH)

            while (cursor.moveToNext()) {
                val size = cursor.getLong(sizeCol)
                // 过滤未写入数据的 pending 空记录（如拍照预创建），与系统相册保持一致
                if (size <= 0L) continue
                val id = cursor.getLong(idCol)
                rows.add(
                    MediaRow(
                        uri = ContentUris.withAppendedId(uri, id).toString(),
                        bucket = cursor.getString(bucketCol) ?: "",
                        displayName = cursor.getString(displayNameCol) ?: "",
                        size = size,
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        mimeType = cursor.getString(mimeTypeCol) ?: "",
                        dateModified = cursor.getLong(dateModifiedCol),
                    )
                )
            }
        }
        rows
    }

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
                val mimeType = FileUtils.getFileMimeType(file)
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
            val mimeType = FileUtils.getFileMimeType(file)
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
                OsFileUtils.copy(fis, out)
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
            FileUtils.quickClose(fis)
            FileUtils.quickClose(out)
        }
    }

    /** 为拍照预创建 MediaStore 图片 Uri（pending 状态，写入数据前对相册不可见）。失败返回空串。 */
    fun createImageUri(context: Context): String {
        val pictureName = "${System.currentTimeMillis()}.jpg"
        return try {
            val values = ContentValues().apply {
                put(DISPLAY_NAME, pictureName)
                put(MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            context.contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?.toString()
                ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "createImageUri failed", e)
            ""
        }
    }

    /** 拍照成功后清除 pending 状态，使照片对系统相册与查询可见。 */
    fun finishImageUri(context: Context, uri: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri.toUri(), values, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "finishImageUri failed", e)
        }
    }

    /** 删除预创建但未写入数据的图片记录（拍照取消/失败时清理，避免残留透明图）。 */
    fun deleteImageUri(context: Context, uri: String) {
        try {
            context.contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Images.Media._ID} = ?",
                arrayOf(uri.substringAfterLast('/')),
            )
        } catch (e: Exception) {
            Log.e(TAG, "deleteImageUri failed", e)
        }
    }
}
