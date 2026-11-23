package com.example.fragmject.core.android.platform.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.fragmject.core.android.platform.log.LogSanitizer

/**
 * 按目标宽度解码文件路径对应的 Bitmap，带 inSampleSize 降采样。
 */
fun Context.getBitmapFromPath(path: String, targetWidth: Int = 0): Bitmap? {
    try {
        val option = BitmapFactory.Options()
        if (targetWidth != 0) {
            option.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, option)
            option.inSampleSize = calculateInSampleSize(option.outWidth, targetWidth)
            option.inJustDecodeBounds = false
        }
        option.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeFile(path, option)
    } catch (e: Exception) {
        Log.e("BitmapDecoder", "getBitmapFromPath failed: ${LogSanitizer.path(path)}", e)
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
    return try {
        if (targetWidth == 0) {
            val option = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, option) }
        } else {
            decodeScaledFromUri(uri, targetWidth)
        }
    } catch (e: Exception) {
        Log.e("BitmapDecoder", "getBitmapFromUri failed: $uri", e)
        null
    }
}

/**
 * 按目标宽度解码：P+ 用 [ImageDecoder] 精确缩放，以下用 inSampleSize 降采样。
 */
private fun Context.decodeScaledFromUri(uri: Uri, targetWidth: Int): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder 支持直接按目标宽采样，比 inSampleSize 更精确
        return ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri)
        ) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            // 必须按原始宽高比换算目标高度：只传 targetWidth 时高度沿用原值，
            // 而 setTargetSize(w, w) 会把非方形图压成方形（编辑画布里的拉伸变形即由此产生）。
            val srcWidth = info.size.width
            val srcHeight = info.size.height
            if (srcWidth > 0) {
                // 宽度对齐 targetWidth（与旧行为一致），高度按原始比例换算
                val ratio = targetWidth.toFloat() / srcWidth
                decoder.setTargetSize(targetWidth, (srcHeight * ratio).toInt().coerceAtLeast(1))
            }
        }
    }
    val option = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    @Suppress("DEPRECATION")
    // 仅用于读取尺寸（inJustDecodeBounds），流必须关闭；真实解码在下方重新打开
    contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, option)
    }
    option.inSampleSize = calculateInSampleSize(option.outWidth, targetWidth)
    option.inJustDecodeBounds = false
    option.inPreferredConfig = Bitmap.Config.ARGB_8888
    return contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, option)
    }
}

/**
 * 计算 [BitmapFactory.Options.inSampleSize]。
 *
 * 取 `源宽 / 目标宽` 的整数部分：解码结果的宽度仍不小于目标宽（不会糊），
 * 同时避免 sqrt 启发式在缩放比较小时退化为 1——例如 4032 像素宽的图按 1080 目标
 * 用 sqrt 算出 1.93 取整为 1，等于不做降采样，直接按原图占内存。
 */
private fun calculateInSampleSize(sourceWidth: Int, targetWidth: Int): Int {
    if (sourceWidth <= 0 || targetWidth <= 0) return 1
    return (sourceWidth / targetWidth).coerceAtLeast(1)
}
