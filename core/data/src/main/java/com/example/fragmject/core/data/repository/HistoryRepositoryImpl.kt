package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.database.store.HistoryStore
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.model.History
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HistoryRepository] 领域端口的 data 层适配器。
 *
 * 内部操作 database 的 HistoryStore，将 HistoryEntity 映射为领域模型 History。
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyStore: HistoryStore,
) : HistoryRepository {

    override fun observeBookmarks(): Flow<List<History>> =
        historyStore.getBookmark().map { list -> list.map { it.toDomain() } }

    override fun observeBrowseHistory(): Flow<List<History>> =
        historyStore.getBrowseHistory().map { list -> list.map { it.toDomain() } }

    override fun observeSearchHistory(): Flow<List<History>> =
        historyStore.getSearchHistory().map { list -> list.map { it.toDomain() } }

    override suspend fun setBookmark(value: String, url: String) =
        historyStore.setBookmark(value, url)

    override suspend fun setBrowseHistory(value: String, url: String) =
        historyStore.setBrowseHistory(value, url)

    override suspend fun setSearchHistory(value: String) =
        historyStore.setSearchHistory(value)

    override suspend fun deleteHistory(history: History) =
        historyStore.deleteHistory(history.toEntity())
}
