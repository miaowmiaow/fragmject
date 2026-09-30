package com.example.fragmject.core.model

/**
 * 分享文章响应容器。
 *
 * 从 [Article] 域聚合中拆出：语义是「分享」而非「文章」，
 * 且被 network / data-contract 层独立引用。
 */
data class ShareArticle(
    val coinInfo: Coin? = null,
    val shareArticles: ArticleData? = null,
)
