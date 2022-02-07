package com.example.fragment.module.wan.bean

import android.os.Parcelable
import com.example.fragment.library.base.http.HttpResponse
import kotlinx.parcelize.Parcelize

@Parcelize
class UpdateBean(
    val data: UpdateDataBean? = null
) : HttpResponse(), Parcelable

@Parcelize
data class UpdateDataBean(
    val versionCode: Long,
    val versionName: String,
    val download_url: String
) : Parcelable
