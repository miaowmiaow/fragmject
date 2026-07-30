package com.example.fragmject.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "project_tree",
    indices = [
        Index(value = ["cache_key", "projectId"], unique = true),
        Index(value = ["cache_key", "sort_order"]),
    ]
)
data class ProjectTreeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "projectId") val projectId: String,
    @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,

    val name: String = "",
    @ColumnInfo(name = "course_id") val courseId: String = "",
    val order: String = "",
    @ColumnInfo(name = "parent_chapter_id") val parentChapterId: String = "",
    @ColumnInfo(name = "user_control_set_top") val userControlSetTop: String = "",
    val visible: String = "",
    val timestamp: Long = 0L,
)
