package com.example.fragmject.core.domain.result

import com.example.fragmject.core.model.Coin

/** 积分排行分页数据。 */
data class CoinRankPageData(
    val coins: List<Coin>,
    val pageCount: Int?,
)
