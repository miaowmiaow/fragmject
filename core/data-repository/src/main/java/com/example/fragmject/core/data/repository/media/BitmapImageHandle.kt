package com.example.fragmject.core.data.repository.media

import android.graphics.Bitmap
import com.example.fragmject.core.domain.media.ImageHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 平台位图的 [ImageHandle] 实现：位图本体只在本模块内持有。
 *
 * 设计为 public 而非 internal：UI 侧（如图片编辑/裁剪画布）持有真实
 * [android.graphics.Bitmap]，需要把合成结果交给 [com.example.fragmject.core.domain.media.MediaEditor]，
 * 因此必须能构造本句柄。除此之外 UI 不接触 data 层任何类型。
 */
class BitmapImageHandle(val bitmap: Bitmap) : ImageHandle {

    override val width: Int get() = bitmap.width

    override val height: Int get() = bitmap.height

    override suspend fun obtainPngBytes(): ByteArray? = withContext(Dispatchers.Default) {
        runCatching {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }.getOrNull()
    }
}
