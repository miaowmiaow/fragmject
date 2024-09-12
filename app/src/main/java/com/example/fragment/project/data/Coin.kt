package com.example.fragment.project.data

import android.os.Parcelable
import android.util.Log
import com.example.fragment.project.R
import com.example.miaow.base.http.HttpResponse
import com.google.gson.Gson
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.abs

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

    @IgnoredOnParcel
    val avatarList: List<Int> = listOf(
        R.mipmap.avatar_1_raster,
        R.mipmap.avatar_2_raster,
        R.mipmap.avatar_3_raster,
        R.mipmap.avatar_4_raster,
        R.mipmap.avatar_5_raster,
        R.mipmap.avatar_6_raster,
    )

    @IgnoredOnParcel
    val avatarId by lazy {
        try {
            avatarList[abs(userId.toInt()) % 6]
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            1
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

}