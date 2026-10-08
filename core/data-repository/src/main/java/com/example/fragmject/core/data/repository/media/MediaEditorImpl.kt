package com.example.fragmject.core.data.repository.media

import android.content.Context
import android.net.Uri
import com.example.fragmject.core.android.platform.getBitmapFromPath
import com.example.fragmject.core.android.platform.getBitmapFromUri
import com.example.fragmject.core.domain.media.EditedImage
import com.example.fragmject.core.domain.media.ImageHandle
import com.example.fragmject.core.domain.media.ImageSource
import com.example.fragmject.core.domain.media.MediaEditor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MediaEditor] 的 data 层实现。
 *
 * 只负责与平台相关的部分：按来源解码位图并包装为 [BitmapImageHandle]；
 * 保存的业务规则（有效性校验、编码、落盘判定）委托给 [MediaSaveRules]，
 * 后者不依赖 Context，可在纯 JVM 下测试。
 */
@Singleton
class MediaEditorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val saveRules: MediaSaveRules,
) : MediaEditor {

    override val saveProgress: Flow<Float> = saveRules.saveProgress

    override suspend fun loadBitmap(source: ImageSource, targetWidth: Int): ImageHandle? =
        withContext(Dispatchers.IO) {
            val bitmap = when (source) {
                is ImageSource.Path -> context.getBitmapFromPath(source.value, targetWidth)
                is ImageSource.Uri -> context.getBitmapFromUri(Uri.parse(source.value), targetWidth)
            }
            // 业务规则：解码失败或退化成 1×1 的位图一律视为无效
            bitmap?.takeIf { it.width > 1 && it.height > 1 }?.let(::BitmapImageHandle)
        }

    override suspend fun saveImage(handle: ImageHandle): EditedImage? = saveRules.save(handle)
}
