package com.example.fragmject.core.model

import com.example.fragmject.core.network.http.HttpResponse

data class NavigationList(
    val data: MutableList<Navigation>? = null
) : HttpResponse()

data class Navigation(
    val articles: MutableList<Article>? = null,
    val cid: String = "",
    val name: String = ""
)
