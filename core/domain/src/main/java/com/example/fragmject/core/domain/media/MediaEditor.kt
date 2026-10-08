package com.example.fragmject.core.domain.media

import kotlinx.coroutines.flow.Flow

/**
 * 图片来源：文件路径或 content Uri（平台无关字符串）。
 */
sealed interface ImageSource {
    data class Path(val value: String) : ImageSource
    data class Uri(val value: String) : ImageSource
}

/**
 * 编辑导出产物：[uriString] 保证非空。
 */
data class EditedImage(val path: String, val uriString: String)

/**
 * 图片编辑领域端口：位图加载 + 编辑结果落盘。
 *
 * 与 [com.example.fragmject.core.domain.repository.MediaRepository] 的分工：
 * - [MediaRepository] 是「落盘原语」（把已就绪的字节写入相册、管理 MediaStore Uri）；
 * - [MediaEditor] 是「编辑业务能力」（解码、有效性校验、编码、成功判定），
 *   业务规则收敛在此，不再散落在 UI。
 *
 * 线程约定（实现需保证）：
 * - [loadBitmap] 内部切 IO，调用方可在主线程调用；
 * - [saveImage] 内部依次完成有效性校验 → 编码（Default）→ 落盘（由 MediaRepository 切 IO）。
 *
 * 业务规则（由实现承担，UI 不再重复判断）：
 * - 解码失败、或尺寸 ≤ 1 的位图一律视为无效，返回 null；
 * - 落盘失败、或结果 uri 为空，一律返回 null，绝不返回 1×1 空白图。
 */
interface MediaEditor {

    /** 按目标宽度加载位图（targetWidth = 0 表示不降采样）。 */
    suspend fun loadBitmap(source: ImageSource, targetWidth: Int = 0): ImageHandle?

    /** 保存编辑结果；成功返回 [EditedImage]，失败返回 null。 */
    suspend fun saveImage(handle: ImageHandle): EditedImage?

    /** 保存进度 0f~1f，供 UI 显示「保存中」。 */
    val saveProgress: Flow<Float>
}
