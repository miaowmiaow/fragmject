package com.example.fragment.project.bean

import com.example.fragment.library.base.R
import com.example.fragment.library.base.http.HttpResponse
import com.google.gson.Gson
import kotlinx.parcelize.IgnoredOnParcel

data class LoginBean(
    val data: UserBean? = null
) : HttpResponse()

data class RegisterBean(
    val data: UserBean? = null
) : HttpResponse()

data class UserBean @JvmOverloads constructor(
    val admin: String = "",
    val chapterTops: List<Any>? = null,
    var coinCount: String = "",
    val collectIds: List<Int>? = null,
    val email: String = "",
    val icon: String = "",
    val id: String = "",
    val nickname: String = "",
    val password: String = "",
    val publicName: String = "",
    val token: String = "",
    val type: String = "",
    var avatar: String = "",
    var sex: String = "",
    var birthday: String = "",
    var city: String = "",
    val username: String = ""
) {

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
            val id = id.toInt()
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