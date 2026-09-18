package com.example.fragmject.core.model

/**
 * 历史/书签/搜索记录领域模型，无 Room 依赖。
 *
 * 与 [com.example.fragmject.core.database.model.HistoryEntity]（Room Entity）对应，
 * 通过 key 区分数据域（bookmark / browse_history / search_history）。
 */
data class History(
    val id: Long,
    val key: String,
    val value: String,
    val url: String = "",
)
