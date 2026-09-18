package com.example.fragmject.core.model

data class Navigation(
    val articles: MutableList<Article>? = null,
    val cid: String = "",
    val name: String = ""
)
