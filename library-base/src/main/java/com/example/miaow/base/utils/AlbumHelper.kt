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
    val savePath = CacheUtils.getDirPath(this, Environment.DIRECTORY_PICTURES)
    val fileName = url.encodeUtf8().md5().hex()
    CoroutineScope(Dispatchers.Main).launch {
        download(savePath, fileName) {
            setUrl(url)
        }
        withContext(Dispatchers.IO) {
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
                        ) ?: Uri.EMPTY
                        out = contentResolver.openOutputStream(uri) ?: return@withContext
                        fis = FileInputStream(file)
                        FileUtils.copy(fis, out)
                        MainThreadExecutor.get().execute {
                            onFinish.invoke(getBitmapPathFromUri(uri), uri ?: Uri.EMPTY)
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
                    Log.e(this.javaClass.name, e.message.toString())
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
            Log.e(this.javaClass.name, e.message.toString())
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
            Log.e(this.javaClass.name, e.message.toString())
        } finally {
            fis?.close()
            out?.close()
        }
    }.start()
}