package com.example.fragmject.core.domain.result

/**
 * 文件下载领域结果。
 */
data class DownloadResult(
    val success: Boolean,
    val message: String = "",
)
