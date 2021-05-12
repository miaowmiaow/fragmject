package com.example.fragment.user.bean

import com.example.fragment.library.base.http.HttpResponse

data class UserCoinBean(
    val data: CoinBean? = null
) : HttpResponse()

data class MyCoinListBean(
    val data: MyCoinDataBean? = null
) : HttpResponse()

data class MyCoinDataBean(
    val curPage: String = "",
    val datas: List<MyCoinBean>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "",
    val size: String = "",
    val total: String = ""
)

data class MyCoinBean(
    val coinCount: String = "",
    val date: String = "",
    val desc: String = "",
    val id: String = "",
    val reason: String = "",
    val type: String = "",
    val userId: String = "",
    val username: String = ""
)

data class CoinBean(
    val coinCount: String = "",
    val level: String = "",
    val nickname: String = "",
    val rank: String = "",
    val userId: String = "",
    val username: String = ""
)