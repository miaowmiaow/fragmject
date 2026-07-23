package com.example.fragment.project.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    indices = [
        Index(value = ["key", "id"]),
        Index(value = ["key", "value", "id"]),
        Index(value = ["key", "url", "id"]),
    ]
)
data class History(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "url") val url: String = "",
)