package com.example.fragmject.core.model

data class MyCoinData(
    val curPage: String = "",
    val datas: List<MyCoin>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "0",
    val size: String = "",
    val total: String = ""
)

data class MyCoin(
    val date: String = "",
    val desc: String = "",
    val id: String = "",
    val reason: String = "",
    val type: String = "",
) : Coin()

data class CoinRankData(
    val curPage: String = "",
    val datas: List<Coin>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "0",
    val size: String = "",
    val total: String = ""
)

open class Coin @JvmOverloads constructor(
    val coinCount: String = "0",
    val level: String = "",
    val nickname: String = "---",
    val rank: String = "",
    val userId: String = "0",
    var username: String = ""
)