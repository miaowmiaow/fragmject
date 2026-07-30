package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "coin_rank",
    indices = [
        Index(value = ["cache_key", "userId"], unique = true),
        Index(value = ["cache_key", "sort_order"]),
    ]
)
data class CoinRankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,

    val username: String = "",
    val nickname: String = "",
    @ColumnInfo(name = "coin_count") val coinCount: String = "",
    val level: String = "",
    val rank: String = "",
    val timestamp: Long = 0L,
)
