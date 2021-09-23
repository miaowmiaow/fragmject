package com.example.fragment.library.base.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLConnection

object AlbumUtil {

    fun Context.saveSystemAlbum(bitmap: Bitmap, onFinish: (String) -> Unit) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val moviesPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
            val recordPath = moviesPath + File.separator + System.currentTimeMillis() + ".png"
            var fos: FileOutputStream? = null
            try {
                val file = File(recordPath)
                fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                val mimeType = URLConnection.getFileNameMap().getContentTypeFor(file.name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues()
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                    val url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    contentResolver.insert(url, values)?.let { uri ->
                        var out: OutputStream? = null
                        var fis: FileInputStream? = null
                        try {
                            out = contentResolver.openOutputStream(uri)
                            fis = FileInputStream(file)
                            if (out != null) {
                                FileUtils.copy(fis, out)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            fis?.close()
                            out?.close()
                        }
                    }
                    onFinish.invoke(file.absolutePath)
                } else {
                    MediaScannerConnection.scanFile(
                        this,
                        arrayOf(file.absolutePath),
                        arrayOf(mimeType)
                    ) { path, _ ->
                        onFinish.invoke(path)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }
        }
    }

}