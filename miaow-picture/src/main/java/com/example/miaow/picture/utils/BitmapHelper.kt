package com.example.miaow.picture.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import java.io.IOException

fun Context.getBitmapFromPath(path: String, targetDensity: Int): Bitmap? {
    try {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, bitmapOptions)
        val bitmapWidth = bitmapOptions.outWidth
        bitmapOptions.inJustDecodeBounds = false
        bitmapOptions.inScaled = true
        bitmapOptions.inDensity = bitmapWidth
        bitmapOptions.inTargetDensity = targetDensity
        return BitmapFactory.decodeFile(path, bitmapOptions)
    } catch (e: Exception) {
        e.printStackTrace()
        val text = "请确认存储权限。\n" + e.message
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
    return null
}

fun Context.getBitmapFromUri(uri: Uri, targetDensity: Int): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val scheme = uri.scheme
        if (ContentResolver.SCHEME_CONTENT == scheme || ContentResolver.SCHEME_FILE == scheme) {
            try {
                val src = ImageDecoder.createSource(contentResolver, uri)
                return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    val bitmapWidth = info.size.width
                    val bitmapHeight = info.size.height
                    val density = targetDensity.toFloat() / bitmapWidth.toFloat()
                    decoder.setTargetSize(
                        (bitmapWidth * density).toInt(),
                        (bitmapHeight * density).toInt()
                    )
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: IOException) {
                e.printStackTrace()
                val text = "请确认存储权限。\n" + e.message
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    return null
}

fun Context.getBitmapPathFromUri(uri: Uri): String {
    var imagePath = ""
    if (DocumentsContract.isDocumentUri(this, uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        if ("com.android.providers.media.documents" == uri.authority) {
            val id = docId.split(":".toRegex()).toTypedArray()[1]
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection = MediaStore.Images.Media._ID + "=" + id
            imagePath = crQueryPath(contentUri, selection)
        } else if ("com.android.providers.downloads.documents" == uri.authority) {
            val uriString = "content://downloads/public_downloads"
            val contentUri = ContentUris.withAppendedId(Uri.parse(uriString), docId.toLong())
            imagePath = crQueryPath(contentUri)
        }
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        imagePath = crQueryPath(uri)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        imagePath = uri.path.toString()
    }
    return imagePath
}

fun Context.crQueryPath(uri: Uri, selection: String = ""): String {
    val cursor = contentResolver.query(uri, null, selection, null, null)
    var path = ""
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            path = cursor.getString(index)
        }
        cursor.close()
    }
    return path
}