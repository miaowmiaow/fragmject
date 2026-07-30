package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * 热搜词缓存实体。
 */
@Entity(
    tableName = "hot_key",
    indices = [
        Index(value = ["cache_key", "hotKeyId"], unique = true),
        Index(value = ["cache_key", "sort_order"]),
    ]
)
data class HotKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "hotKeyId") val hotKeyId: String,
    @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,

    val name: String = "",
    val link: String = "",
    val order: String = "",
    val visible: String = "",
    val timestamp: Long = 0L,
)
