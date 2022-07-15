package com.example.fragment.library.base.utils

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
import kotlin.math.sqrt

//java.lang.RuntimeException: Canvas: trying to draw too large(xxx bytes) bitmap.
//该异常由 android.graphics.RecordingCanvas.java 或 android.view.DisplayListCanvas.java （SDK版本差异）的 throwIfCannotDraw(Bitmap bitmap) 抛出。
//阅读源码可知加载的图片内存大小超过 MAX_BITMAP_SIZE，因此控制图片的加载内存即可解决。
//此处为什么限制为64 MB？ 因为64 = 100 0000 = 2^6 = 2^3 * 2^3 = 8 * 8 即八八六十四，又因8谐音“发”，故有发发之意 ~。~ (ps: 纯属瞎扯淡，逗大家一乐)
const val MAX_BITMAP_SIZE = 64f * 1024 * 1024 // 64 MB

fun Context.getBitmapFromPath(path: String, targetWidth: Int = 0): Bitmap? {
    try {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, bitmapOptions)
        val bitmapWidth = bitmapOptions.outWidth
        val bitmapHeight = bitmapOptions.outHeight
        //bitmapSize = 图片宽度 * 图片高度 * 色彩模式 （ARGB_8888 = 4byte）
        val bitmapSize = bitmapWidth * bitmapHeight * 4
        if (bitmapSize > MAX_BITMAP_SIZE || targetWidth > 0) {
            val maxWidth = (bitmapWidth * sqrt(MAX_BITMAP_SIZE / bitmapSize)).toInt()
            bitmapOptions.inJustDecodeBounds = false
            bitmapOptions.inScaled = true
            bitmapOptions.inDensity = bitmapWidth
            bitmapOptions.inTargetDensity = targetWidth.coerceAtMost(maxWidth)
        }
        return BitmapFactory.decodeFile(path, bitmapOptions)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
    }
    return null
}

fun Context.getBitmapFromUri(uri: Uri, targetWidth: Int = 0): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val scheme = uri.scheme
        if (ContentResolver.SCHEME_CONTENT == scheme || ContentResolver.SCHEME_FILE == scheme) {
            try {
                val src = ImageDecoder.createSource(contentResolver, uri)
                return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    val bitmapWidth = info.size.width
                    val bitmapHeight = info.size.height
                    //bitmapSize = 图片宽度 * 图片高度 * 色彩模式 （ARGB_8888 = 4byte）
                    val bitmapSize = bitmapWidth * bitmapHeight * 4
                    if (bitmapSize > MAX_BITMAP_SIZE || targetWidth > 0) {
                        val maxWidth = bitmapWidth * sqrt(MAX_BITMAP_SIZE / bitmapSize)
                        val scale = targetWidth.toFloat().coerceAtMost(maxWidth) / bitmapWidth
                        decoder.setTargetSize(
                            (bitmapWidth * scale).toInt(),
                            (bitmapHeight * scale).toInt()
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
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
            imagePath = contentResolverQueryPath(contentUri, selection)
        } else if ("com.android.providers.downloads.documents" == uri.authority) {
            val uriString = "content://downloads/public_downloads"
            val contentUri = ContentUris.withAppendedId(Uri.parse(uriString), docId.toLong())
            imagePath = contentResolverQueryPath(contentUri)
        }
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        imagePath = contentResolverQueryPath(uri)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        imagePath = uri.path.toString()
    }
    return imagePath
}

fun Context.contentResolverQueryPath(uri: Uri, selection: String = ""): String {
    val cursor = contentResolver.query(uri, null, selection, null, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            return cursor.getString(index)
        }
        cursor.close()
    }
    return ""
}