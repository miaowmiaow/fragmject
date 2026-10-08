package com.example.fragmject.core.android.platform.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlin.math.sqrt

/**
 * 按目标宽度解码文件路径对应的 Bitmap，带 inSampleSize 降采样。
 */
fun Context.getBitmapFromPath(path: String, targetWidth: Int = 0): Bitmap? {
    try {
        val option = BitmapFactory.Options()
        if (targetWidth != 0) {
            option.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, option)
            val scale = (option.outWidth * 1f / targetWidth)
            option.inSampleSize = if (scale > 1) sqrt(scale).toInt() else 1
            option.inJustDecodeBounds = false
        }
        option.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeFile(path, option)
    } catch (e: Exception) {
        Log.e("BitmapDecoder", "getBitmapFromPath failed: $path", e)
    }
    return null
}

/**
 * 按目标宽度解码 URI 对应的 Bitmap。
 *
 * Android P+ 走 [ImageDecoder]（按目标宽精确缩放），以下走 inSampleSize 降采样。
 */
fun Context.getBitmapFromUri(uri: Uri, targetWidth: Int = 0): Bitmap? {
    if (uri == Uri.EMPTY) return null
    try {
        val option = BitmapFactory.Options()
        if (targetWidth != 0) {
            option.inJustDecodeBounds = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // ImageDecoder 支持直接按目标宽采样，比 inSampleSize 更精确
                return ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(contentResolver, uri)
                ) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    // 按 targetWidth 等比缩放，避免加载全尺寸原图
                    decoder.setTargetSize(targetWidth, targetWidth)
                }
            } else {
                @Suppress("DEPRECATION")
                // 仅用于读取尺寸（inJustDecodeBounds），流必须关闭；真实解码在下方 68 行再次打开
                contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, option)
                }
            }
            val scale = (option.outWidth * 1f / targetWidth)
            option.inSampleSize = if (scale > 1) sqrt(scale).toInt() else 1
            option.inJustDecodeBounds = false
        }
        option.inPreferredConfig = Bitmap.Config.ARGB_8888
        contentResolver.openInputStream(uri)?.use {
            return BitmapFactory.decodeStream(it, null, option)
        }
    } catch (e: Exception) {
        Log.e("BitmapDecoder", "getBitmapFromUri failed: $uri", e)
    }
    return null
}
