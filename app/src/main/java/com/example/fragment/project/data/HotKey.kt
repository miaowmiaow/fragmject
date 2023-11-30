package com.example.fragment.project.data

import com.example.miaow.base.http.HttpResponse

data class HotKeyList(
    val data: List<HotKey>? = null
) : HttpResponse()

data class HotKey @JvmOverloads constructor(
    val id: String = "",
    val link: String = "",
    val name: String = "",
    val order: String = "",
    val visible: String = ""
)