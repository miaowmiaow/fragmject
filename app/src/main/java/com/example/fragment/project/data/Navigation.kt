package com.example.fragment.project.data

import com.example.miaow.base.http.HttpResponse

data class NavigationList(
    val data: MutableList<Navigation>? = null
) : HttpResponse()

data class Navigation(
    val articles: MutableList<Article>? = null,
    val cid: String = "",
    val name: String = ""
)
