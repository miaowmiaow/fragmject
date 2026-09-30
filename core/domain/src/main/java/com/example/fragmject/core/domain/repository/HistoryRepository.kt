package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.model.History
import kotlinx.coroutines.flow.Flow

/**
 * 历史 / 书签 / 搜索记录领域端口。
 *
 * 实现由 data 层适配器提供，内部依赖 data-contract 的 HistoryLocalDataSource。
 */
interface HistoryRepository {
    fun observeBookmarks(): Flow<List<History>>
    fun observeBrowseHistory(): Flow<List<History>>
    fun observeSearchHistory(): Flow<List<History>>
    suspend fun setBookmark(value: String, url: String)
    suspend fun recordBrowseVisit(value: String, url: String)
    suspend fun recordSearchQuery(value: String)
    suspend fun deleteHistory(history: History)
}
