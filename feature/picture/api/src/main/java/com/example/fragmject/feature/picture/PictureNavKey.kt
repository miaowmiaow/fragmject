package com.example.fragmject.feature.picture

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Picture Feature — 统一路由表。
 *
 * - [PictureSelectorNavKey]     图片选择器
 * - [PicturePreviewNavKey]      图片预览（可选多选）
 * - [PictureEditorNavKey]       图片编辑
 */

@Serializable
object PictureSelectorNavKey : NavKey

@Serializable
data class PicturePreviewNavKey(val positions: List<Int>) : NavKey

@Serializable
data class PictureEditorNavKey(val oldUriString: String) : NavKey
