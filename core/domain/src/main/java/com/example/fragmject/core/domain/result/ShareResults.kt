package com.example.fragmject.core.domain.result

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin

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

/** 用户分享文章结果。 */
data class UserShareResult(
    val coin: Coin? = null,
    val articles: List<Article> = emptyList(),
    val pageCount: Int? = null,
)
