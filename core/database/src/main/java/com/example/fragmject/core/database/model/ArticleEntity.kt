package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room 实体：缓存的文章列表项。
 *
 * 设计要点：
 * - [articleId] 为玩Android API 的文章原始 id（String），与 [cacheKey] 组成唯一索引，
 *   同一篇文章在不同页面（首页/体系/项目）各自独立缓存；
 * - [tags] 和 [banners] 以 JSON 字符串存储，通过 [ArticleTypeConverters] 序列化；
 * - [sortOrder] 保存列表顺序，查询时按此排序还原；
 * - [timestamp] 用于 TTL 过期清理。
 */
@Entity(
    tableName = "article",
    indices = [
        Index(value = ["cache_key", "articleId"], unique = true),
        Index(value = ["cache_key", "sort_order"]),
    ]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "articleId") val articleId: String,
    @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,

    // ---- 来自 Article ----
    val title: String,
    val author: String = "",
    val link: String = "",
    @ColumnInfo(name = "chapter_name") val chapterName: String = "",
    @ColumnInfo(name = "super_chapter_name") val superChapterName: String = "",
    @ColumnInfo(name = "nice_date") val niceDate: String = "",
    @ColumnInfo(name = "envelope_pic") val envelopePic: String = "",
    val desc: String = "",
    val collect: Boolean = false,
    val fresh: Boolean = false,
    val top: Boolean = false,
    @ColumnInfo(name = "chapter_id") val chapterId: String = "",
    @ColumnInfo(name = "super_chapter_id") val superChapterId: String = "",
    @ColumnInfo(name = "user_id") val userId: String = "",
    @ColumnInfo(name = "share_user") val shareUser: String = "",
    val zan: String = "",

    // ---- JSON 序列化的嵌套对象 ----
    @ColumnInfo(name = "tags_json") val tagsJson: String = "",
    @ColumnInfo(name = "banners_json") val bannersJson: String = "",

    // ---- 其他（不全量持久化，仅缓存核心字段） ----
    val timestamp: Long = 0L,
)
