package com.example.fragment.project.bean

import android.os.Parcelable
import com.example.fragment.library.base.R
import com.example.fragment.library.base.http.HttpResponse
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
    val coinCount: String = "",
    val level: String = "",
    val nickname: String = "",
    val rank: String = "",
    val userId: String = "",
    var username: String = ""
) : Parcelable {

    @IgnoredOnParcel
    val avatarList: List<Int> = listOf(
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster,
    )

    fun getAvatarId(): Int {
        var index = 0
        try {
            val id = userId.toInt()
            if (id >= 0) {
                index = id % 6
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return avatarList[index]
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

}