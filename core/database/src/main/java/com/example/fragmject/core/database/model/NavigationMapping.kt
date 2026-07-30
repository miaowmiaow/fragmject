package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Navigation
import com.google.gson.reflect.TypeToken

fun Navigation.toEntity(cacheKey: String, sortOrder: Int): NavigationEntity {
    val gson = ArticleTypeConverters.gson
    return NavigationEntity(
        navId = cid.ifEmpty { name },
        cacheKey = cacheKey,
        sortOrder = sortOrder,
        cid = cid,
        name = name,
        articlesJson = articles?.let { gson.toJson(it) } ?: "",
        timestamp = System.currentTimeMillis(),
    )
}

fun NavigationEntity.toDomain(): Navigation {
    val gson = ArticleTypeConverters.gson
    return Navigation(
        cid = cid,
        name = name,
        articles = articlesJson.takeIf { it.isNotEmpty() }?.let {
            gson.fromJson(it, object : TypeToken<MutableList<Article>>() {}.type)
        },
    )
}
