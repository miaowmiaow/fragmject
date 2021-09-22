package com.example.fragment.library.base.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URLConnection

object AlbumUtil {

    fun saveSystemAlbum(context: Context, file: File, onFinish: (String) -> Unit) {
        val fileNameMap = URLConnection.getFileNameMap()
        val mimeType = fileNameMap.getContentTypeFor(file.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fileName = file.name
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            val url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.insert(url, values)?.let { uri ->
                var out: OutputStream? = null
                var fis: FileInputStream? = null
                try {
                    out = context.contentResolver.openOutputStream(uri)
                    fis = FileInputStream(file)
                    if (out != null) {
                        FileUtils.copy(fis, out)
                    }
                } catch (e: IOException) {
                    e.printStackTrace();
                } finally {
                    fis?.close()
                    out?.close()
                }
            }
            onFinish.invoke(file.absolutePath)
        } else {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.path),
                arrayOf(mimeType)
            ) { path, _ ->
                onFinish.invoke(path)
            }
        }
    }

}