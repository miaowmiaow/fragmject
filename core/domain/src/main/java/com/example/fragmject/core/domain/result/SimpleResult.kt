package com.example.fragmject.core.domain.result

/**
 * 轻量通用结果：表达「成功 / 失败 + 可读消息」。
 *
 * 收敛原先结构完全相同的分享、收藏、下载等结果类型，
 * 供分享、收藏、下载等领域端口统一复用。
 */
data class SimpleResult(
    val success: Boolean,
    val message: String = "",
)
