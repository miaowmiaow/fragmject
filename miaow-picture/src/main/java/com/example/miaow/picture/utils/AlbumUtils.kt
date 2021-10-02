package com.example.miaow.picture.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLConnection

object AlbumUtils {

    fun Context.saveSystemAlbum(bitmap: Bitmap, onFinish: (String) -> Unit) {
        Thread {
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
                            MainThreadExecutor.get().execute {
                                onFinish.invoke(getImagePath(uri))
                            }
                        }
                    } else {
                        MediaScannerConnection.scanFile(
                            this,
                            arrayOf(file.absolutePath),
                            arrayOf(mimeType)
                        ) { path, _ ->
                            MainThreadExecutor.get().execute {
                                onFinish.invoke(path)
                            }
                        }
                    }
                    file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    fos?.close()
                }
            }
        }.start()
    }

    fun Context.getImagePath(uri: Uri): String {
        var imagePath = ""
        if (DocumentsContract.isDocumentUri(this, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.providers.media.documents" == uri.authority) {
                val id = docId.split(":".toRegex()).toTypedArray()[1]
                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val selection = MediaStore.Images.Media._ID + "=" + id
                imagePath = contentResolverQuery(contentUri, selection)
            } else if ("com.android.providers.downloads.documents" == uri.authority) {
                val uriString = "content://downloads/public_downloads"
                val contentUri = ContentUris.withAppendedId(Uri.parse(uriString), docId.toLong())
                imagePath = contentResolverQuery(contentUri)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            imagePath = contentResolverQuery(uri)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            imagePath = uri.path.toString()
        }
        return imagePath
    }

    private fun Context.contentResolverQuery(uri: Uri, selection: String = ""): String {
        val cursor = this.contentResolver.query(uri, null, selection, null, null)
        var path = ""
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path
    }

}