package com.example.miaow.base.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
import com.example.miaow.base.http.download
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

private const val TAG_ALBUM = "AlbumHelper"

fun Context.saveImagesToAlbum(url: String, onFinish: (String, Uri) -> Unit) {
    val savePath = CacheUtils.getDirPath(this, Environment.DIRECTORY_PICTURES)
    val fileName = url.encodeUtf8().md5().hex()
    AppScope.launch {
        download(savePath, fileName) {
            setUrl(url)
        }
        val file = File(savePath, fileName)
        if (file.exists() && file.isFile) {
            var out: OutputStream? = null
            var fis: FileInputStream? = null
            try {
                val mimeType = FileUtil.getFileMimeType(file)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues()
                    values.put(DISPLAY_NAME, file.name)
                    values.put(MIME_TYPE, mimeType)
                    values.put(RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    val uri = contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    ) ?: return@launch
                    out = contentResolver.openOutputStream(uri) ?: return@launch
                    fis = FileInputStream(file)
                    FileUtils.copy(fis, out)
                    MainThreadExecutor.get().execute {
                        // uri 在前面已通过 ?: return@launch 保证非空，无需再次回退
                        onFinish.invoke(getBitmapPathFromUri(uri), uri)
                    }
                } else {
                    val paths = arrayOf(file.absolutePath)
                    val mimeTypes = arrayOf(mimeType)
                    MediaScannerConnection.scanFile(
                        this@saveImagesToAlbum,
                        paths,
                        mimeTypes
                    ) { path, uri ->
                        MainThreadExecutor.get().execute {
                            onFinish.invoke(path ?: "", uri ?: Uri.EMPTY)
                        }
                    }
                }
                file.delete()
            } catch (e: Exception) {
                Log.e(TAG_ALBUM, "saveImagesToAlbum(url) failed: $url", e)
            } finally {
                // close 自身可能抛异常，使用 quickClose 避免影响后续 close
                quickCloseInternal(fis)
                quickCloseInternal(out)
            }
        }
    }
}

fun Context.saveImagesToAlbum(bitmap: Bitmap, onFinish: (String, Uri) -> Unit) {
    // 之前直接 Thread {}.start() 没有任何生命周期管控，统一改用 AppScope
    AppScope.launch {
        var fos: FileOutputStream? = null
        var out: OutputStream? = null
        var fis: FileInputStream? = null
        try {
            val pictureName = "${System.currentTimeMillis()}.png"
            val cachePath = CacheUtils.getDirPath(this@saveImagesToAlbum, Environment.DIRECTORY_PICTURES)
            val file = File(cachePath, pictureName)
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            val mimeType = FileUtil.getFileMimeType(file)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(DISPLAY_NAME, file.name)
                values.put(MIME_TYPE, mimeType)
                values.put(RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                val uri =
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@launch
                out = contentResolver.openOutputStream(uri) ?: return@launch
                fis = FileInputStream(file)
                FileUtils.copy(fis, out)
                MainThreadExecutor.get().execute {
                    onFinish.invoke(getBitmapPathFromUri(uri), uri)
                }
            } else {
                val paths = arrayOf(file.absolutePath)
                val mimeTypes = arrayOf(mimeType)
                MediaScannerConnection.scanFile(this@saveImagesToAlbum, paths, mimeTypes) { path, uri ->
                    MainThreadExecutor.get().execute {
                        onFinish.invoke(path ?: "", uri ?: Uri.EMPTY)
                    }
                }
            }
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG_ALBUM, "saveImagesToAlbum(bitmap) failed", e)
        } finally {
            quickCloseInternal(fos)
            quickCloseInternal(fis)
            quickCloseInternal(out)
        }
    }
}

fun Context.saveVideoToAlbum(file: File, onFinish: (String, Uri) -> Unit) {
    AppScope.launch {
        var out: OutputStream? = null
        var fis: FileInputStream? = null
        try {
            val mimeType = FileUtil.getFileMimeType(file)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(DISPLAY_NAME, file.name)
                values.put(MIME_TYPE, mimeType)
                values.put(RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                val url = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri = contentResolver.insert(url, values) ?: return@launch
                out = contentResolver.openOutputStream(uri) ?: return@launch
                fis = FileInputStream(file)
                FileUtils.copy(fis, out)
                MainThreadExecutor.get().execute {
                    onFinish.invoke(getBitmapPathFromUri(uri), uri)
                }
            } else {
                val paths = arrayOf(file.absolutePath)
                val mimeTypes = arrayOf(mimeType)
                MediaScannerConnection.scanFile(this@saveVideoToAlbum, paths, mimeTypes) { path, uri ->
                    MainThreadExecutor.get().execute {
                        onFinish.invoke(path ?: "", uri ?: Uri.EMPTY)
                    }
                }
            }
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG_ALBUM, "saveVideoToAlbum failed: ${file.absolutePath}", e)
        } finally {
            quickCloseInternal(fis)
            quickCloseInternal(out)
        }
    }
}
