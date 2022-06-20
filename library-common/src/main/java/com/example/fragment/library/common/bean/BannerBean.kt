package com.example.fragment.library.common.bean

import android.os.Parcelable
import com.example.fragment.library.base.http.HttpResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class BannerListBean(
    val data: List<BannerBean>? = null
) : HttpResponse(), Parcelable

@Parcelize
data class BannerBean(
    val desc: String = "",
    val id: String = "",
    val imagePath: String = "",
    val isVisible: String = "",
    val order: String = "",
    val title: String = "",
    val type: String = "",
    val url: String = ""
) : Parcelable