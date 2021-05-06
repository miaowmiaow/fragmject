package com.example.fragment.module.home.bean

import com.example.fragment.library.base.http.HttpResponse

data class ConfigBean(
    val data: ConfigDataBean? = null
) : HttpResponse()

data class ConfigDataBean(
    val _enableAtDate_: String? = null,
    val _secondFloorBgImageUrl_backup: String? = null,
    val _theme_: String? = null,
    val actionBarBgColor: String? = null,
    val actionBarBgImageUrl: String? = null,
    val enableAtDate: List<String>? = null,
    val grayFilter: Boolean = false,
    val homeTitle: String? = null,
    val secondFloorBgImageBlurPercent: Double = 0.0,
    val secondFloorBgImageUrl: String? = null,
    val theme: String? = null
)
