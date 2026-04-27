package com.example.fragment.project.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class History(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "url") val url: String = "",
)