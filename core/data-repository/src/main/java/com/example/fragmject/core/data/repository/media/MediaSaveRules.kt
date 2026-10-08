package com.example.fragmject.core.data.repository.media

import com.example.fragmject.core.domain.media.EditedImage
import com.example.fragmject.core.domain.media.ImageHandle
import com.example.fragmject.core.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 编辑保存的业务规则（与 Context 无关，可纯 JVM 单测）。
 *
 * 收敛原先散落在 UI 的判断，避免各页面判定口径不一致：
 * - 尺寸非法的位图（如未就绪时合成出的 1×1）不得进入保存流程；
 * - 编码失败不得继续落盘；
 * - 落盘结果必须 uri 非空才算成功（曾出现 success=true 但 uri 为空）。
 */
// internal：仅由本模块 MediaEditorImpl 注入使用，
// 对外不暴露（架构测试规则 16 约束 media 包的对外表面）
@Singleton
internal class MediaSaveRules @Inject constructor(
    private val mediaRepository: MediaRepository,
) {

    private val _saveProgress = MutableStateFlow(0f)
    val saveProgress: Flow<Float> = _saveProgress.asStateFlow()

    suspend fun save(handle: ImageHandle): EditedImage? {
        _saveProgress.value = 0f
        return try {
            if (handle.width <= 1 || handle.height <= 1) return null
            val bytes = handle.obtainPngBytes() ?: return null
            _saveProgress.value = 0.6f
            val result = mediaRepository.saveImageToAlbum(bytes)
            if (result.success && result.uri.isNotBlank()) {
                EditedImage(path = result.path, uriString = result.uri)
            } else {
                null
            }
        } finally {
            _saveProgress.value = 1f
        }
    }
}
