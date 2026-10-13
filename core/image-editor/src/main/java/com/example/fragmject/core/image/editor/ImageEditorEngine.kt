package com.example.fragmject.core.image.editor

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.example.fragmject.core.android.platform.media.getBitmapFromPath
import com.example.fragmject.core.android.platform.media.getBitmapFromUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图片保存结果：[path] 为缓存文件路径，[uri] 为相册 uri（url）。
 */
data class SaveResult(val path: String, val uri: String)

/**
 * 图片编辑引擎：对外提供「位图加载 + 编辑保存」闭环能力。
 *
 * feature 只与本引擎交互，返回 [SaveResult]（含相册 uri）；不再直接接触
 * 平台解码（android-platform）或数据实现（data-repository）的任何类型。
 */
@Singleton
class ImageEditorEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val saveCoordinator: ImageSaveCoordinator,
) {

    /** 保存进度 0f~1f，供 UI 显示「保存中」。 */
    val saveProgress: Flow<Float> = saveCoordinator.saveProgress

    /**
     * 按来源加载位图（path 与 uri 二选一，优先 path），在 IO 线程解码。
     * 解码失败或尺寸 ≤ 1 的位图一律视为无效，返回 null。
     */
    suspend fun loadBitmap(path: String?, uri: String?, targetWidth: Int = 0): Bitmap? =
        withContext(Dispatchers.IO) {
            val bitmap = when {
                path != null -> context.getBitmapFromPath(path, targetWidth)
                uri != null -> context.getBitmapFromUri(uri.toUri(), targetWidth)
                else -> null
            }
            bitmap?.takeIf { it.width > 1 && it.height > 1 }
        }

    /** 保存位图；成功返回 [SaveResult]，失败返回 null。 */
    suspend fun save(bitmap: Bitmap): SaveResult? = saveCoordinator.save(bitmap)
}
