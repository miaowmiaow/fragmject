package com.example.fragment.project.bean

import com.example.fragment.library.base.http.HttpResponse

data class HotKeyListBean(
    val data: List<HotKeyBean>? = null
) : HttpResponse()

data class HotKeyBean @JvmOverloads constructor(
    val id: String = "",
    val link: String = "",
    val name: String = "",
    val order: String = "",
    val visible: String = ""
)