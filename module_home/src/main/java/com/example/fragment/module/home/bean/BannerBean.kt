package com.example.fragment.module.home.bean

import com.example.fragment.library.base.http.HttpResponse

data class BannerBean(
    val data: List<BannerDataBean>? = null
) : HttpResponse()

data class BannerDataBean(
    val desc: String? = null,
    val id: Int? = null,
    val imagePath: String? = null,
    val isVisible: Int? = null,
    val order: Int? = null,
    val title: String? = null,
    val type: Int? = null,
    val url: String? = null
)