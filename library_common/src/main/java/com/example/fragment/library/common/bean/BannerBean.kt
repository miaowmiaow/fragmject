package com.example.fragment.library.common.bean

import com.example.fragment.library.base.http.HttpResponse

data class BannerBean(
    val data: List<BannerDataBean>? = null
) : HttpResponse()

data class BannerDataBean(
    val desc: String = "",
    val id: String = "",
    val imagePath: String = "",
    val isVisible: String = "",
    val order: String = "",
    val title: String = "",
    val type: String = "",
    val url: String = ""
)