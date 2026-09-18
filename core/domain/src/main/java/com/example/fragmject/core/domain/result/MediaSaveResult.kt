package com.example.fragmject.core.domain.result

/**
 * 媒体保存（图片/视频到系统相册）领域结果。
 */
data class MediaSaveResult(
    val success: Boolean,
    val path: String = "",
    val uri: String = "",
)
