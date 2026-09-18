package com.example.fragmject.core.domain.result

import com.example.fragmject.core.model.Article

/** 加载更多返回的数据：文章列表 + 总页数（用于分页判断）。 */
data class PageData(
    val articles: List<Article>,
    val pageCount: Int?,
)

/** 搜索返回的文章分页结果。 */
data class ArticlePageResult(
    val articles: List<Article> = emptyList(),
    val pageCount: Int? = null,
)
