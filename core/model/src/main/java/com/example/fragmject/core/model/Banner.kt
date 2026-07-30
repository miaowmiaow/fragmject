package com.example.fragmject.core.model

import android.os.Parcelable
import com.example.fragmject.core.network.http.HttpResponse
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