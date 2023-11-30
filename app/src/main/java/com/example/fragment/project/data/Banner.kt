package com.example.fragment.project.data

import android.os.Parcelable
import com.example.miaow.base.http.HttpResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class BannerList(
    val data: List<Banner>? = null
) : HttpResponse(), Parcelable

@Parcelize
data class Banner(
    val desc: String = "",
    val id: String = "",
    val imagePath: String = "",
    val isVisible: String = "",
    val order: String = "",
    val title: String = "",
    val type: String = "",
    val url: String = ""
) : Parcelable