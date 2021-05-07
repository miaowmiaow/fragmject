package com.example.fragment.project.bean

import com.example.fragment.library.base.http.HttpResponse

data class HotKeyBean(
    val data: List<HotKeyDataBean>? = null
) : HttpResponse()

data class HotKeyDataBean(
    val id: String = "",
    val link: String = "",
    val name: String = "",
    val order: String = "",
    val visible: String = ""
)