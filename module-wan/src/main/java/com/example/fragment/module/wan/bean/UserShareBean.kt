package com.example.fragment.module.user.bean

import android.os.Parcelable
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.common.bean.ArticleDataBean
import com.example.fragment.library.common.bean.CoinBean
import kotlinx.parcelize.Parcelize

@Parcelize
class UserShareBean(
    val data: UserShareDataBean? = null
) : HttpResponse(), Parcelable

@Parcelize
data class UserShareDataBean(
    val coinInfo: CoinBean? = null,
    val shareArticles: ArticleDataBean? = null,
) : Parcelable
