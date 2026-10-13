package com.example.fragmject.core.image.editor

import android.graphics.Bitmap
import com.example.fragmject.core.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图片保存业务规则（纯 JVM 可测部分：有效性校验 + PNG 编码 + 落盘判定）。
 *
 * 收敛原先散落在 UI 层的判断，避免各调用点判定口径不一致：
 * - 尺寸非法的位图（如未就绪时合成出的 1×1）不得进入保存流程；
 * - 编码失败不得继续落盘；
 * - 落盘结果必须 uri 非空才算成功（曾出现 success=true 但 uri 为空）。
 *
 * 仅由同模块 [ImageEditorEngine] 注入使用；对外经引擎暴露，业务方不应直接依赖本类。
 */
@Singleton
class ImageSaveCoordinator @Inject constructor(
    private val mediaRepository: MediaRepository,
) {

    private val _saveProgress = MutableStateFlow(0f)
    val saveProgress: Flow<Float> = _saveProgress.asStateFlow()

    /** 保存位图；成功返回 [SaveResult]，失败返回 null。 */
    suspend fun save(bitmap: Bitmap): SaveResult? =
        saveInternal(bitmap.width, bitmap.height) { compressPng(bitmap) }

    /**
     * 核心保存流程（纯 JVM 可测，不依赖 android.graphics.Bitmap）：
     * - 尺寸非法（宽或高 ≤ 1）拒绝；
     * - 编码失败（[encode] 返回 null）拒绝；
     * - 落盘结果 uri 非空才算成功（曾出现 success=true 但 uri 为空）。
     *
     * internal：供同模块单测直接验证业务规则，测试用 lambda 模拟编码结果。
     */
    internal suspend fun saveInternal(
        width: Int,
        height: Int,
        encode: () -> ByteArray?,
    ): SaveResult? {
        _saveProgress.value = 0f
        return try {
            if (width <= 1 || height <= 1) return null
            val bytes = encode() ?: return null
            _saveProgress.value = 0.6f
            val result = mediaRepository.saveImageToAlbum(bytes)
            if (result.success && result.uri.isNotBlank()) SaveResult(result.path, result.uri) else null
        } finally {
            _saveProgress.value = 1f
        }
    }

    /** PNG 编码；编码失败返回 null。 */
    private fun compressPng(bitmap: Bitmap): ByteArray? = runCatching {
        val baos = ByteArrayOutputStream()
        if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)) baos.toByteArray() else null
    }.getOrNull()
}
