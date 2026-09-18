package com.example.fragmject.core.domain.result

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin

/** 我分享的文章分页结果。 */
data class MyShareResult(
    val articles: List<Article> = emptyList(),
    val pageCount: Int? = null,
    val hasMore: Boolean = false,
)

/** 积分首页结果。 */
data class MyCoinHomeResult(
    val coin: Coin? = null,
    val coinList: List<MyCoin> = emptyList(),
    val pageCount: Int? = null,
)

/** 积分明细下一页结果。 */
data class MyCoinNextResult(
    val items: List<MyCoin> = emptyList(),
    val pageCount: Int? = null,
    val isEmpty: Boolean = true,
)
