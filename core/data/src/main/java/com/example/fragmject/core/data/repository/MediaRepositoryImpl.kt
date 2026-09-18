package com.example.fragmject.core.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.example.fragmject.core.common.utils.AppScope
import com.example.fragmject.core.common.utils.CacheUtils
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.result.MediaSaveResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

/**
 * 媒体保存 data 实现：编排「下载/落盘 → 写相册」完整流程，
 * 纯 MediaStore 写入委托给 [MediaStoreUtils]，不再依赖 network 层的 AlbumHelper。
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
) : MediaRepository {

    override fun saveImageToAlbum(url: String, onFinish: (MediaSaveResult) -> Unit) {
        AppScope.launch {
            val savePath = CacheUtils.getDirPath(context, Environment.DIRECTORY_PICTURES)
            val fileName = "image_${System.currentTimeMillis()}"
            val result = downloadRepository.download(url, savePath, fileName)
            if (!result.success) {
                Log.w(TAG, "download image failed: $url, msg=${result.message}")
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = false))
                }
                return@launch
            }
            val file = File(savePath, fileName)
            if (file.exists() && file.isFile) {
                val (path, uri) = MediaStoreUtils.saveImage(context, file)
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = true, path = path, uri = uri))
                }
            } else {
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = false))
                }
            }
        }
    }

    override fun saveImageToAlbum(imageBytes: ByteArray, onFinish: (MediaSaveResult) -> Unit) {
        AppScope.launch {
            val pictureName = "image_${System.currentTimeMillis()}.png"
            val cachePath = CacheUtils.getDirPath(context, Environment.DIRECTORY_PICTURES)
            val file = File(cachePath, pictureName)
            try {
                file.writeBytes(imageBytes)
                val (path, uri) = MediaStoreUtils.saveImage(context, file)
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = true, path = path, uri = uri))
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveImageToAlbum(bytes) failed", e)
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = false))
                }
            }
        }
    }

    override fun saveBase64ImageToAlbum(base64: String, onFinish: (MediaSaveResult) -> Unit) {
        AppScope.launch {
            try {
                // 兼容 data:image/png;base64,xxxx 前缀：提取首个逗号后的纯 base64 内容
                val pure = base64.substringAfter(",", base64)
                val bytes = Base64.decode(pure, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalArgumentException("decode bitmap failed")
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)

                val pictureName = "image_${System.currentTimeMillis()}.png"
                val cachePath = CacheUtils.getDirPath(context, Environment.DIRECTORY_PICTURES)
                val file = File(cachePath, pictureName)
                file.writeBytes(baos.toByteArray())
                val (path, uri) = MediaStoreUtils.saveImage(context, file)
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = true, path = path, uri = uri))
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveBase64ImageToAlbum failed", e)
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = false))
                }
            }
        }
    }

    override fun saveVideoToAlbum(filePath: String, onFinish: (MediaSaveResult) -> Unit) {
        AppScope.launch {
            val file = File(filePath)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = false))
                }
                return@launch
            }
            val (path, uri) = MediaStoreUtils.saveVideo(context, file)
            withContext(Dispatchers.Main) {
                onFinish(MediaSaveResult(success = true, path = path, uri = uri))
            }
        }
    }

    override fun notifyMediaAdded(filePath: String, onFinish: (MediaSaveResult) -> Unit) {
        AppScope.launch {
            val file = File(filePath)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    onFinish(MediaSaveResult(success = false))
                }
                return@launch
            }
            val (path, uri) = MediaStoreUtils.notifyAdded(context, file)
            withContext(Dispatchers.Main) {
                onFinish(MediaSaveResult(success = true, path = path, uri = uri))
            }
        }
    }

    override fun createImageUri(): String {
        val pictureName = "${System.currentTimeMillis()}.jpg"
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, pictureName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    // 标记为 pending：写入数据前对相册与 MediaStore 查询不可见，避免透明图
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )?.toString() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "createImageUri failed", e)
            ""
        }
    }

    override fun finishImageUri(uri: String) {
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

    override fun deleteImageUri(uri: String) {
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

    private companion object {
        const val TAG = "MediaRepository"
    }
}