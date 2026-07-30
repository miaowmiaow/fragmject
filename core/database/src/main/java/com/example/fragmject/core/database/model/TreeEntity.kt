package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * 体系树缓存实体。children 以 JSON 存储避免递归外键。
 */
@Entity(
    tableName = "tree",
    indices = [
        Index(value = ["cache_key", "treeId"], unique = true),
        Index(value = ["cache_key", "sort_order"]),
    ]
)
data class TreeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "treeId") val treeId: String,
    @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,

    val name: String = "",
    @ColumnInfo(name = "course_id") val courseId: String = "",
    val order: String = "",
    @ColumnInfo(name = "parent_chapter_id") val parentChapterId: String = "",
    @ColumnInfo(name = "user_control_set_top") val userControlSetTop: String = "",
    val visible: String = "",

    /** 子节点 JSON 数组（递归 Tree 结构）。 */
    @ColumnInfo(name = "children_json") val childrenJson: String = "",
    val timestamp: Long = 0L,
)
