package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * 导航缓存实体。articles 以 JSON 存储。
 */
@Entity(
    tableName = "navigation",
    indices = [
        Index(value = ["cache_key", "navId"], unique = true),
        Index(value = ["cache_key", "sort_order"]),
    ]
)
data class NavigationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "navId") val navId: String,
    @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,

    val cid: String = "",
    val name: String = "",

    /** articles JSON 数组。 */
    @ColumnInfo(name = "articles_json") val articlesJson: String = "",
    val timestamp: Long = 0L,
)
