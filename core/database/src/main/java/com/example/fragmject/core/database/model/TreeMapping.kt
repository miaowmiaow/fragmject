package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.Tree
import com.google.gson.reflect.TypeToken

fun Tree.toEntity(cacheKey: String, sortOrder: Int): TreeEntity {
    val gson = ArticleTypeConverters.gson
    return TreeEntity(
        treeId = id,
        cacheKey = cacheKey,
        sortOrder = sortOrder,
        name = name,
        courseId = courseId,
        order = order,
        parentChapterId = parentChapterId,
        userControlSetTop = userControlSetTop,
        visible = visible,
        childrenJson = children?.let { gson.toJson(it) } ?: "",
        timestamp = System.currentTimeMillis(),
    )
}

fun TreeEntity.toDomain(): Tree {
    val gson = ArticleTypeConverters.gson
    return Tree(
        id = treeId,
        name = name,
        courseId = courseId,
        order = order,
        parentChapterId = parentChapterId,
        userControlSetTop = userControlSetTop,
        visible = visible,
        children = childrenJson.takeIf { it.isNotEmpty() }?.let {
            gson.fromJson(it, object : TypeToken<List<Tree>>() {}.type)
        },
    )
}
