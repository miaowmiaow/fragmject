package com.example.fragmject.core.data.contract.local

import com.example.fragmject.core.model.History
import kotlinx.coroutines.flow.Flow

/**
 * 历史 / 书签 / 搜索记录本地数据源。
 *
 * 由 core:database 使用 Room DAO/Entity/Mapping 实现；core:data 只依赖本契约，
 * 不感知 Room DAO、Entity 或 Store。
 */
interface HistoryLocalDataSource {
    fun observeBookmarks(): Flow<List<History>>
    fun observeBrowseHistory(): Flow<List<History>>
    fun observeSearchHistory(): Flow<List<History>>
    suspend fun setBookmark(value: String, url: String)
    suspend fun setBrowseHistory(value: String, url: String)
    suspend fun setSearchHistory(value: String)
    suspend fun deleteHistory(history: History)
}
