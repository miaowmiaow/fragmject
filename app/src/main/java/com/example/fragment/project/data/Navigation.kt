package com.example.fragment.project.data

import com.example.miaow.base.http.HttpResponse

data class NavigationList(
    val data: List<Navigation>? = null
) : HttpResponse()

data class Navigation(
    val articles: List<Article>? = null,
    val cid: String = "",
    val name: String = "",
    var isSelected: Boolean = false
)
