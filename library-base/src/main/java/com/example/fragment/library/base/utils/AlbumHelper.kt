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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLConnection

fun Context.saveSystemAlbum(bitmap: Bitmap, onFinish: (String?, Uri?) -> Unit) {
    Thread {
        var fos: FileOutputStream? = null
        var out: OutputStream? = null
        var fis: FileInputStream? = null
        try {
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return@Thread
            val pictureDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return@Thread
            val imageFile = File(pictureDir, "${System.currentTimeMillis()}.png")
            fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            val mimeType = URLConnection.getFileNameMap().getContentTypeFor(imageFile.name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(DISPLAY_NAME, imageFile.name)
                values.put(MIME_TYPE, mimeType)
                values.put(RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                val url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = contentResolver.insert(url, values) ?: return@Thread
                out = contentResolver.openOutputStream(uri) ?: return@Thread
                fis = FileInputStream(imageFile)
                FileUtils.copy(fis, out)
                MainThreadExecutor.get().execute {
                    onFinish.invoke(getBitmapPathFromUri(uri), uri)
                }
            } else {
                val paths = arrayOf(imageFile.absolutePath)
                val mimeTypes = arrayOf(mimeType)
                MediaScannerConnection.scanFile(this, paths, mimeTypes) { path, uri ->
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
            fis?.close()
            out?.close()
        }
    }.start()
}