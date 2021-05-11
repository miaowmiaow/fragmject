package com.example.fragment.user.bean

import com.example.fragment.library.base.http.HttpResponse
import com.google.gson.Gson

data class LoginBean(
    val data: UserBean? = null
) : HttpResponse()

data class UserBean(
    val admin: String = "",
    val chapterTops: List<Any>? = null,
    val coinCount: String = "",
    val collectIds: List<Int>? = null,
    val email: String = "",
    val icon: String = "",
    val id: String = "",
    val nickname: String = "",
    val password: String = "",
    val publicName: String = "",
    val token: String = "",
    val type: String = "",
    val username: String = ""
) {

    fun toJson(): String {
        return Gson().toJson(this)
    }

}