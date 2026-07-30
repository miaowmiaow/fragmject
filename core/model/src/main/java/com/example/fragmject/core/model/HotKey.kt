package com.example.fragmject.core.model

import com.example.fragmject.core.network.http.HttpResponse

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