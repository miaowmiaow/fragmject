package com.example.fragmject.core.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.fragmject.core.android.platform.media.BitmapCodec
import com.example.fragmject.core.android.platform.cache.CacheUtils
import com.example.fragmject.core.android.platform.media.MediaStoreUtils
import com.example.fragmject.core.data.repository.util.discardOnFailure
import com.example.fragmject.core.data.repository.util.guardedMediaSave
import com.example.fragmject.core.domain.repository.DownloadRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.domain.result.MediaSaveResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 媒体保存 data 实现：编排「下载/落盘 → 写相册」完整流程，
 * 纯 MediaStore 写入委托给 [MediaStoreUtils]，不再依赖 network 层的 AlbumHelper。
 *
 * 所有保存方法均为 suspend，由调用方（ViewModel）在自身作用域调度，
 * 不再使用进程级 AppCoroutineScope 启动 fire-and-forget 协程。
 *
 * 取消语义：预期失败（IO、解码、下载失败等）返回 `success = false`；
 * 协程取消必须原样抛出，不得转成失败结果（见 [guardedMediaSave]）。
 * 交给 MediaStore 之前被取消或失败时，本类创建的临时文件会被清理（见 [discardOnFailure]）。
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
) : MediaRepository {

    override suspend fun saveImageToAlbum(url: String): MediaSaveResult =
        withContext(Dispatchers.IO) {
            guardedMediaSave(onFailure = { Log.e(TAG, "saveImageToAlbum(url) failed", it) }) {
                val savePath = CacheUtils.getDirPath(context, Environment.DIRECTORY_PICTURES)
                val fileName = "image_${System.currentTimeMillis()}"
                val file = File(savePath, fileName)
                val ready = file.discardOnFailure {
                    val result = downloadRepository.download(url, savePath, fileName)
                    if (!result.success) {
                        Log.w(TAG, "download image failed: $url, msg=${result.message}")
                        file.delete()
                        false
                    } else {
                        // 交接前确认仍未取消：取消在此抛出，由 discardOnFailure 清理文件
                        currentCoroutineContext().ensureActive()
                        file.isFile
                    }
                }
                if (ready) handOffImage(file) else MediaSaveResult(success = false)
            }
        }

    override suspend fun saveImageToAlbum(imageBytes: ByteArray): MediaSaveResult =
        withContext(Dispatchers.IO) {
            guardedMediaSave(onFailure = { Log.e(TAG, "saveImageToAlbum(bytes) failed", it) }) {
                handOffImage(writeTempPng(imageBytes))
            }
        }

    override suspend fun saveBase64ImageToAlbum(base64: String): MediaSaveResult =
        withContext(Dispatchers.IO) {
            guardedMediaSave(onFailure = { Log.e(TAG, "saveBase64ImageToAlbum failed", it) }) {
                val bytes = BitmapCodec.base64ToPngBytes(base64)
                handOffImage(writeTempPng(bytes))
            }
        }

    override suspend fun saveVideoToAlbum(filePath: String): MediaSaveResult =
        withContext(Dispatchers.IO) {
            guardedMediaSave(onFailure = { Log.e(TAG, "saveVideoToAlbum failed", it) }) {
                // 文件归调用方所有，不在此清理
                val file = File(filePath)
                if (!file.exists()) {
                    MediaSaveResult(success = false)
                } else {
                    val (path, uri) = MediaStoreUtils.saveVideo(context, file)
                    MediaSaveResult(success = uri.isNotBlank(), path = path, uri = uri)
                }
            }
        }

    override suspend fun notifyMediaAdded(filePath: String): MediaSaveResult =
        withContext(Dispatchers.IO) {
            guardedMediaSave(onFailure = { Log.e(TAG, "notifyMediaAdded failed", it) }) {
                val file = File(filePath)
                if (!file.exists()) {
                    MediaSaveResult(success = false)
                } else {
                    val (path, uri) = MediaStoreUtils.notifyAdded(context, file)
                    MediaSaveResult(success = uri.isNotBlank(), path = path, uri = uri)
                }
            }
        }

    override suspend fun createImageUri(): String = withContext(Dispatchers.IO) {
        MediaStoreUtils.createImageUri(context)
    }

    override suspend fun finishImageUri(uri: String) {
        withContext(Dispatchers.IO) {
            MediaStoreUtils.finishImageUri(context, uri)
        }
    }

    override suspend fun deleteImageUri(uri: String) {
        withContext(Dispatchers.IO) {
            MediaStoreUtils.deleteImageUri(context, uri)
        }
    }

    /** 把字节写入临时 PNG 文件；写入失败或交接前被取消时删除该文件。 */
    private suspend fun writeTempPng(bytes: ByteArray): File {
        val pictureName = "image_${System.currentTimeMillis()}.png"
        val cachePath = CacheUtils.getDirPath(context, Environment.DIRECTORY_PICTURES)
        val file = File(cachePath, pictureName)
        file.discardOnFailure {
            file.writeBytes(bytes)
            currentCoroutineContext().ensureActive()
        }
        return file
    }

    /**
     * 把临时图片交给 MediaStore。此后文件由 [MediaStoreUtils] 负责删除；
     * 仅当它报告失败（uri 为空）时才由本方法兜底删除，此时扫描已结束，不会与之竞争。
     */
    private suspend fun handOffImage(file: File): MediaSaveResult {
        val (path, uri) = MediaStoreUtils.saveImage(context, file)
        if (uri.isBlank()) file.delete()
        return MediaSaveResult(success = uri.isNotBlank(), path = path, uri = uri)
    }

    private companion object {
        const val TAG = "MediaRepository"
    }
}
