package com.example.fragment.project.bean

import android.os.Parcelable
import android.util.Log
import com.example.fragment.project.R
import com.example.miaow.base.http.HttpResponse
import com.google.gson.Gson
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

data class UserCoinBean(
    val data: CoinBean? = null
) : HttpResponse()

data class MyCoinListBean(
    val data: MyCoinDataBean? = null
) : HttpResponse()

data class CoinRankBean(
    val data: CoinRankDataBean? = null
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
    val date: String = "",
    val desc: String = "",
    val id: String = "",
    val reason: String = "",
    val type: String = "",
) : CoinBean() {

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

data class CoinRankDataBean(
    val curPage: String = "",
    val datas: List<CoinBean>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "",
    val size: String = "",
    val total: String = ""
)

@Parcelize
open class CoinBean @JvmOverloads constructor(
    val coinCount: String = "0",
    val level: String = "",
    val nickname: String = "---",
    val rank: String = "",
    val userId: String = "",
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

    fun getAvatarId(): Int {
        var index = 0
        try {
            val id = userId.toInt()
            if (id >= 0) {
                index = id % 6
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return avatarList[index]
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

}