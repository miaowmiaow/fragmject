package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.contract.local.HistoryLocalDataSource
import com.example.fragmject.core.domain.repository.HistoryRepository
import com.example.fragmject.core.model.History
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HistoryRepository] 领域端口的 data 层适配器。
 *
 * 依赖 data-contract 的 [HistoryLocalDataSource]，不感知 Room DAO / Entity / Store。
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val local: HistoryLocalDataSource,
) : HistoryRepository {

    override fun observeBookmarks(): Flow<List<History>> = local.observeBookmarks()

    override fun observeBrowseHistory(): Flow<List<History>> = local.observeBrowseHistory()

    override fun observeSearchHistory(): Flow<List<History>> = local.observeSearchHistory()

    override suspend fun setBookmark(value: String, url: String) = local.setBookmark(value, url)

    override suspend fun recordBrowseVisit(value: String, url: String) = local.recordBrowseVisit(value, url)

    override suspend fun recordSearchQuery(value: String) = local.recordSearchQuery(value)

    override suspend fun deleteHistory(history: History) = local.deleteHistory(history)
}
