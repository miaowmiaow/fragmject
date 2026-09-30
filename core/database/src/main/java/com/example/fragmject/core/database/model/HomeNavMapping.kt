package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.NavTab
import com.google.gson.reflect.TypeToken

fun NavTab.toEntity(cacheKey: String, sortOrder: Int): HomeNavEntity {
    val gson = JsonConverters.gson
    return HomeNavEntity(
        navId = cid.ifEmpty { name },
        cacheKey = cacheKey,
        sortOrder = sortOrder,
        cid = cid,
        name = name,
        articlesJson = articles?.let { gson.toJson(it) } ?: "",
        timestamp = System.currentTimeMillis(),
    )
}

fun HomeNavEntity.toDomain(): NavTab {
    val gson = JsonConverters.gson
    return NavTab(
        cid = cid,
        name = name,
        articles = articlesJson.takeIf { it.isNotEmpty() }?.let {
            gson.fromJson(it, object : TypeToken<List<Article>>() {}.type)
        },
    )
}
