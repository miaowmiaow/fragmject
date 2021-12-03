package com.example.fragment.module.user.bean

import android.os.Parcelable
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.common.bean.ArticleDataBean
import com.example.fragment.library.common.bean.CoinBean
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
