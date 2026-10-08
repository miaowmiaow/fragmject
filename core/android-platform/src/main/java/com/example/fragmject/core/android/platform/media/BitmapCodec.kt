package com.example.fragmject.core.android.platform.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 图片编解码能力：Base64 / Bitmap 解码与 PNG 重编码。
 *
 * 供 data 层（MediaRepository 实现）复用，避免其直接触碰 android.graphics / android.util.Base64。
 */
object BitmapCodec {

    /**
     * 将 base64（兼容 `data:image/...;base64,` 前缀）解码为 PNG 字节。
     *
     * 解码失败（非法 base64 / 非图片字节）或压缩失败时抛出 [IllegalArgumentException]。
     */
    fun base64ToPngBytes(base64: String): ByteArray {
        val pure = base64.substringAfter(",", base64)
        val bytes = Base64.decode(pure, Base64.NO_WRAP)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("decode bitmap failed")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }
}
