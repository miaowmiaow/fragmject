package com.example.fragment.module.home.bean

import com.example.fragment.library.base.http.HttpResponse

data class BannerBean(
    val data: List<BannerDataBean>? = null
) : HttpResponse()

data class BannerDataBean(
    val desc: String? = null,
    val id: Int = 0,
    val imagePath: String? = null,
    val isVisible: Int = 0,
    val order: Int = 0,
    val title: String? = null,
    val type: Int = 0,
    val url: String? = null
)