package com.example.fragment.module.wan.bean

import android.os.Parcelable
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.common.bean.ArticleDataBean
import com.example.fragment.library.common.bean.CoinBean
import kotlinx.parcelize.Parcelize

@Parcelize
class ShareArticleBean(
    val data: ShareArticleDataBean? = null
) : HttpResponse(), Parcelable

@Parcelize
data class ShareArticleDataBean(
    val coinInfo: CoinBean? = null,
    val shareArticles: ArticleDataBean? = null,
) : Parcelable
