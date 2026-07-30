package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleTag
import com.example.fragmject.core.model.Banner

/**
 * Article ↔ ArticleEntity 转换。
 *
 * [tagsJson] / [bannersJson] 通过 Gson 序列化，
 * 避免为嵌套列表单独建关联表。
 */
fun Article.toEntity(cacheKey: String, sortOrder: Int): ArticleEntity {
    val gson = ArticleTypeConverters.gson
    return ArticleEntity(
        articleId = id,
        cacheKey = cacheKey,
        sortOrder = sortOrder,
        title = title,
        author = author,
        link = link,
        chapterName = chapterName,
        superChapterName = superChapterName,
        niceDate = niceDate,
        envelopePic = envelopePic,
        desc = desc,
        collect = collect,
        fresh = fresh,
        top = top,
        chapterId = chapterId,
        superChapterId = superChapterId,
        userId = userId,
        shareUser = shareUser,
        zan = zan,
        tagsJson = tags?.let { gson.toJson(it) } ?: "",
        bannersJson = banners?.let { gson.toJson(it) } ?: "",
        timestamp = System.currentTimeMillis(),
    )
}

fun ArticleEntity.toDomain(): Article {
    val gson = ArticleTypeConverters.gson
    return Article(
        id = articleId,
        title = title,
        author = author,
        link = link,
        chapterName = chapterName,
        superChapterName = superChapterName,
        niceDate = niceDate,
        envelopePic = envelopePic,
        desc = desc,
        collect = collect,
        fresh = fresh,
        top = top,
        chapterId = chapterId,
        superChapterId = superChapterId,
        userId = userId,
        shareUser = shareUser,
        zan = zan,
        tags = tagsJson.takeIf { it.isNotEmpty() }?.let { gson.fromJson(it, Array<ArticleTag>::class.java)?.toList() },
        banners = bannersJson.takeIf { it.isNotEmpty() }?.let { gson.fromJson(it, Array<Banner>::class.java)?.toList() },
    )
}
