package com.example.fragmject.core.domain.result

/** 新建分享结果。 */
data class ShareArticleResult(
    val success: Boolean,
    val message: String,
)

/** 收藏/取消收藏结果。 */
data class CollectResult(
    val success: Boolean,
    val message: String,
)
