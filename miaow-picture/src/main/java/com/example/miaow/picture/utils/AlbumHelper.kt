package com.example.miaow.picture.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import com.example.fragment.library.base.utils.MainThreadExecutor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLConnection

fun Context.saveSystemAlbum(bitmap: Bitmap, onFinish: (String, Uri) -> Unit) {
    Thread {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return@Thread
        }
        val pictureDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return@Thread
        val imageFile = File(pictureDir, "${System.currentTimeMillis()}.png")
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            val mimeType = URLConnection.getFileNameMap().getContentTypeFor(imageFile.name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFile.name)
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES
                )
                val url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                contentResolver.insert(url, values)?.let { uri ->
                    var out: OutputStream? = null
                    var fis: FileInputStream? = null
                    try {
                        out = contentResolver.openOutputStream(uri)
                        fis = FileInputStream(imageFile)
                        if (out != null) {
                            FileUtils.copy(fis, out)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        fis?.close()
                        out?.close()
                    }
                    MainThreadExecutor.get().execute {
                        onFinish.invoke(getBitmapPathFromUri(uri), uri)
                    }
                }
            } else {
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(imageFile.absolutePath),
                    arrayOf(mimeType)
                ) { path, uri ->
                    MainThreadExecutor.get().execute {
                        onFinish.invoke(path, uri)
                    }
                }
            }
            imageFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos?.close()
        }
    }.start()
}