package com.example.fragmject.core.model

import android.os.Parcelable
import com.example.fragmject.core.network.http.HttpResponse
import com.example.fragmject.core.network.utils.GSonUtils
import kotlinx.parcelize.Parcelize

data class UserCoin(
    val data: Coin? = null
) : HttpResponse()

data class MyCoinList(
    val data: MyCoinData? = null
) : HttpResponse()

data class CoinRank(
    val data: CoinRankData? = null
) : HttpResponse()

data class MyCoinData(
    val curPage: String = "",
    val datas: List<MyCoin>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "0",
    val size: String = "",
    val total: String = ""
)

@Parcelize
data class MyCoin(
    val date: String = "",
    val desc: String = "",
    val id: String = "",
    val reason: String = "",
    val type: String = "",
) : Coin() {

    private fun getFirstSpace(): Int {
        return desc.indexOf(" ")
    }

    private fun getSecondSpace(): Int {
        return desc.indexOf(" ", getFirstSpace() + 1)
    }

    fun getTime(): String {
        return desc.substring(0, getSecondSpace())
    }

    fun getTitle(): String {
        return desc.substring(getSecondSpace() + 1)
    }
}

data class CoinRankData(
    val curPage: String = "",
    val datas: List<Coin>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "0",
    val size: String = "",
    val total: String = ""
)

@Parcelize
open class Coin @JvmOverloads constructor(
    val coinCount: String = "0",
    val level: String = "",
    val nickname: String = "---",
    val rank: String = "",
    val userId: String = "0",
    var username: String = ""
) : Parcelable {

    fun toJson(): String {
        return GSonUtils.gson.toJson(this)
    }

}