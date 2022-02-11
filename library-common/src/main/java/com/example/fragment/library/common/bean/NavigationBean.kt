package com.example.fragment.library.common.bean

import com.example.fragment.library.base.http.HttpResponse

data class NavigationListBean(
    val data: List<NavigationBean>? = null
) : HttpResponse()

data class NavigationBean(
    val articles: List<ArticleBean>? = null,
    val cid: String = "",
    val name: String = "",
    var isSelected: Boolean = false
)
