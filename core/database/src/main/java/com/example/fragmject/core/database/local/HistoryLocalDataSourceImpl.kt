package com.example.fragmject.core.database.local

import com.example.fragmject.core.database.dao.HistoryDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.contract.local.HistoryLocalDataSource
import com.example.fragmject.core.model.History
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HistoryLocalDataSource] 的 Room 适配器实现。
 *
 * 按 key 划分书签 / 浏览历史 / 搜索记录，对外只暴露领域模型 [History]，
 * Room Entity 不泄漏到 data 层。
 */
@Singleton
class HistoryLocalDataSourceImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryLocalDataSource {

    private companion object {
        const val KEY_BOOKMARK = "bookmark"
        const val KEY_BROWSE_HISTORY = "browse_history"
        const val KEY_SEARCH_HISTORY = "search_history"
    }

    override fun observeBookmarks(): Flow<List<History>> =
        historyDao.getByKey(KEY_BOOKMARK).map { list -> list.map { it.toDomain() } }

    override fun observeBrowseHistory(): Flow<List<History>> =
        historyDao.getByKey(KEY_BROWSE_HISTORY).map { list -> list.map { it.toDomain() } }

    override fun observeSearchHistory(): Flow<List<History>> =
        historyDao.getByKey(KEY_SEARCH_HISTORY).map { list -> list.map { it.toDomain() } }

    // 「查询 → 删除旧记录 → 插入」的原子性由 DAO 的 @Transaction 方法保证：
    // @Transaction 只在 Room 生成的 DAO 实现中生效，标注在本类方法上无效。
    override suspend fun setBookmark(value: String, url: String) {
        historyDao.replaceByUrl(key = KEY_BOOKMARK, value = value, url = url)
    }

    override suspend fun recordBrowseVisit(value: String, url: String) {
        historyDao.replaceByUrl(key = KEY_BROWSE_HISTORY, value = value, url = url)
    }

    override suspend fun recordSearchQuery(value: String) {
        historyDao.replaceByValue(key = KEY_SEARCH_HISTORY, value = value)
    }

    override suspend fun deleteHistory(history: History) {
        historyDao.delete(history.toEntity())
    }
}
