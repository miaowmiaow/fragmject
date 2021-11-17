package com.example.fragment.module.wan.bean

import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.common.bean.ArticleBean

data class NavigationListBean(
    val data: List<NavigationBean>? = null
) : HttpResponse()

data class NavigationBean(
    val articles: List<ArticleBean>? = null,
    val cid: String = "",
    val name: String = "",
    var isSelected: Boolean = false
)
