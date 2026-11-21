package com.example.fragmject.core.android.platform.media


import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils as OsFileUtils
import android.provider.MediaStore
import android.util.Log
import com.example.fragmject.core.android.platform.file.FileUtils
import androidx.core.net.toUri
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellableContinuation
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

    /**
     * 按系统版本分流：Q+ 走 pending 记录写入，Q 以下交给 MediaScanner 扫描已落盘文件。
     * 异常统一在此兜底（结果为空串），是 [save] 唯一的分支与错误处理点。
     */
    private suspend fun save(
        context: Context,
        file: File,
        collection: Uri,
        relativePath: String,
    ): Pair<String, String> = suspendCancellableCoroutine { cont ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithPendingRecord(context, file, collection, relativePath, cont)
            } else {
                scanSavedFile(context, file, cont)
            }
        } catch (e: Exception) {
            Log.e(TAG, "save failed: ${file.absolutePath}", e)
            if (cont.isActive) cont.resume("" to "")
        }
    }

    /**
     * Q+ 路径：先插入 pending 记录再写数据，写完才解除 pending。
     * 任一步失败/取消都回收已插入的记录，避免相册留下空条目或半截文件。
     * 结果通过 [cont] 投递，异常抛给 [save] 统一兜底。
     */
    private fun saveWithPendingRecord(
        context: Context,
        file: File,
        collection: Uri,
        relativePath: String,
        cont: CancellableContinuation<Pair<String, String>>,
    ) {
        var out: OutputStream? = null
        var fis: FileInputStream? = null
        // 已 insert 的 MediaStore 记录；只有真正写完数据（published）才保留
        var insertedUri: Uri? = null
        var published = false
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.ImageColumns.DISPLAY_NAME, file.name)
                put(MediaStore.Images.ImageColumns.MIME_TYPE, FileUtils.getFileMimeType(file))
                put(MediaStore.Images.ImageColumns.RELATIVE_PATH, relativePath)
                // 先以 pending 状态插入：数据写完前对相册不可见，
                // 失败时删除该记录即可，不会在相册留下空条目
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values)
            if (uri == null) {
                file.delete()
                if (cont.isActive) cont.resume("" to "")
                return
            }
            insertedUri = uri
            out = context.contentResolver.openOutputStream(uri)
            if (out == null) {
                file.delete()
                if (cont.isActive) cont.resume("" to "")
                return
            }
            fis = FileInputStream(file)
            OsFileUtils.copy(fis, out)
            // 先关流再发布，避免系统收到通知时数据还没刷完
            FileUtils.quickClose(fis)
            fis = null
            FileUtils.quickClose(out)
            out = null
            // 复制期间被取消：不发布，finally 里删除记录，避免留下半截文件
            if (!cont.isActive) {
                file.delete()
                return
            }
            // 解除 pending 才算发布成功；失败则记录对相册不可见，按失败回收
            published = publishImageRecord(context, uri)
            if (!published) {
                file.delete()
                if (cont.isActive) cont.resume("" to "")
                return
            }
            file.delete()
            if (cont.isActive) cont.resume(context.getBitmapPathFromUri(uri) to uri.toString())
        } finally {
            FileUtils.quickClose(fis)
            FileUtils.quickClose(out)
            // 任一步失败/取消都回收已插入的记录（未发布 = 数据没写完）
            if (!published) insertedUri?.let { deleteMediaRecord(context, it) }
        }
    }

    /** Q 以下路径：文件已在公共目录，通知系统扫描使其在相册可见。 */
    private fun scanSavedFile(
        context: Context,
        file: File,
        cont: CancellableContinuation<Pair<String, String>>,
    ) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(FileUtils.getFileMimeType(file)),
        ) { path, uri ->
            file.delete()
            if (cont.isActive) cont.resume((path ?: "") to (uri?.toString() ?: ""))
        }
    }

    /** 删除一条已插入的 MediaStore 记录（失败不影响主流程，仅记日志）。 */
    private fun deleteMediaRecord(context: Context, uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteMediaRecord failed: $uri", e)
        }
    }

    /** 为拍照预创建 MediaStore 图片 Uri（pending 状态，写入数据前对相册不可见）。失败返回空串。 */
    fun createImageUri(context: Context): String {
        val pictureName = "${System.currentTimeMillis()}.jpg"
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.ImageColumns.DISPLAY_NAME, pictureName)
                put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.ImageColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
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

    /**
     * 拍照成功后清除 pending 状态，使照片对系统相册与查询可见。
     *
     * @return 是否发布成功；失败时调用方必须删除该记录（[deleteImageUri]），
     * 否则它永远停留在 pending —— 相册看不到，用户也没有入口清理。
     */
    fun finishImageUri(context: Context, uri: String): Boolean {
        // Q 以下没有 pending 概念，无需发布
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || publishImageRecord(context, uri.toUri())
    }

    /** 清除 pending 状态；返回是否成功（失败仅记日志，由调用方决定是否回收记录）。 */
    private fun publishImageRecord(context: Context, uri: Uri): Boolean {
        return try {
            val row = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.IS_PENDING),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) to cursor.getInt(1) else null
            } ?: return false
            if (row.first <= 0L) return false
            // 重试必须幂等：进程可能在成功发布后、清除持久状态前退出。
            if (row.second == 0) return true
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            Log.e(TAG, "finishImageUri failed: $uri", e)
            false
        }
    }

    /** 删除图片记录；已经不存在时按幂等成功处理，异常时返回 false 供调用方保留重试信息。 */
    fun deleteImageUri(context: Context, uri: String): Boolean = try {
        context.contentResolver.delete(uri.toUri(), null, null)
        true
    } catch (e: Exception) {
        Log.e(TAG, "deleteImageUri failed", e)
        false
    }
}
