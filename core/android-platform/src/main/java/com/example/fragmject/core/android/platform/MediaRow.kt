package com.example.fragmject.core.android.platform

/**
 * MediaStore 查询的行模型：对媒体库游标行字段的中立封装（平台层）。
 *
 * 由 [MediaStoreUtils.queryImages] 填充，data-repository 据此分组映射为领域模型，
 * 不直接触碰 MediaStore 常量或 ContentUris。
 *
 * [uri] 为 content Uri 字符串，供领域模型直接使用。
 */
data class MediaRow(
    val uri: String,
    val bucket: String,
    val displayName: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val dateModified: Long,
)
