package com.example.fragmject.feature.picture

import com.example.fragmject.core.navigation.contract.FlowScopedNavKey
import kotlinx.serialization.Serializable

/**
 * Picture Feature — 统一路由表。
 *
 * - [PictureSelectorNavKey]     图片选择器
 * - [PicturePreviewNavKey]      图片预览（可选多选）
 * - [PictureEditorNavKey]       图片编辑
 *
 * 三条路由属于同一条多步流程，统一实现 [FlowScopedNavKey]，供 app 组合根识别
 * 「图片流程是否仍在返回栈中」。三者都是全屏覆盖层，**故意不实现** `DetailPaneNavKey`
 * （大屏不做详情面板分栏）。
 */

@Serializable
object PictureSelectorNavKey : FlowScopedNavKey

@Serializable
data class PicturePreviewNavKey(
    val uris: List<String>,
    /** true = 选图流程态（带缩略图条、选择框与编辑入口）；false = 纯看图。 */
    val selectable: Boolean = false,
) : FlowScopedNavKey

@Serializable
data class PictureEditorNavKey(val oldUriString: String) : FlowScopedNavKey
