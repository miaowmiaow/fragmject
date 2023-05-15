package com.example.fragment.library.base.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.*
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.download
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

fun Context.saveImagesToAlbum(url: String, onFinish: (String, Uri) -> Unit) {
    val cachePath = CacheUtils.getDirPath(this, Environment.DIRECTORY_PICTURES)
    val filePathName = cachePath + File.separator + url.encodeUtf8().md5().hex()
    CoroutineScope(Dispatchers.Main).launch {
        download(HttpRequest(url), filePathName)
        withContext(Dispatchers.IO) {
            val file = File(filePathName)
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
                        val uri =
                            contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values
                            )
                                ?: Uri.EMPTY
                        out = contentResolver.openOutputStream(uri) ?: return@withContext
                        fis = FileInputStream(file)
                        FileUtils.copy(fis, out)
                        MainThreadExecutor.get().execute {
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
                                onFinish.invoke(path, uri)
                            }
                        }
                    }
                    file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    fis?.close()
                    out?.close()
                }
            }
        }
    }
}

fun Context.saveImagesToAlbum(bitmap: Bitmap, onFinish: (String, Uri) -> Unit) {
    Thread {
        var fos: FileOutputStream? = null
        var out: OutputStream? = null
        var fis: FileInputStream? = null
        try {
            val pictureName = "${System.currentTimeMillis()}.png"
            val cachePath = CacheUtils.getDirPath(this, Environment.DIRECTORY_PICTURES)
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
                        ?: Uri.EMPTY
                out = contentResolver.openOutputStream(uri) ?: return@Thread
                fis = FileInputStream(file)
                FileUtils.copy(fis, out)
                MainThreadExecutor.get().execute {
                    onFinish.invoke(getBitmapPathFromUri(uri), uri)
                }
            } else {
                val paths = arrayOf(file.absolutePath)
                val mimeTypes = arrayOf(mimeType)
                MediaScannerConnection.scanFile(this, paths, mimeTypes) { path, uri ->
                    MainThreadExecutor.get().execute {
                        onFinish.invoke(path, uri)
                    }
                }
            }
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos?.close()
            fis?.close()
            out?.close()
        }
    }.start()
}

fun Context.saveVideoToAlbum(file: File, onFinish: (String, Uri) -> Unit) {
    Thread {
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
                val uri = contentResolver.insert(url, values) ?: return@Thread
                out = contentResolver.openOutputStream(uri) ?: return@Thread
                fis = FileInputStream(file)
                FileUtils.copy(fis, out)
                MainThreadExecutor.get().execute {
                    onFinish.invoke(getBitmapPathFromUri(uri), uri)
                }
            } else {
                val paths = arrayOf(file.absolutePath)
                val mimeTypes = arrayOf(mimeType)
                MediaScannerConnection.scanFile(this, paths, mimeTypes) { path, uri ->
                    MainThreadExecutor.get().execute {
                        onFinish.invoke(path, uri)
                    }
                }
            }
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fis?.close()
            out?.close()
        }
    }.start()
}