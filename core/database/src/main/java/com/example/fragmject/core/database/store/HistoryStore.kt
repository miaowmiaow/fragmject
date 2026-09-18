package com.example.fragmject.core.database.store

import com.example.fragmject.core.database.dao.HistoryDao
import com.example.fragmject.core.database.model.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书签 / 历史 / 搜索记录持久化存储 facade。
 *
 * 直接封装 [com.example.fragmject.core.database.dao.HistoryDao]，按 key 划分数据域。
 * ViewModel 可直接注入或引用本对象，无需经过中间层。
 */
@Singleton
class HistoryStore @Inject constructor(
    private val historyDao: HistoryDao,
) {

    private companion object {
        const val KEY_BOOKMARK = "bookmark"
        const val KEY_BROWSE_HISTORY = "browse_history"
        const val KEY_SEARCH_HISTORY = "search_history"
    }

    /** ---- 书签 ---- */
    suspend fun setBookmark(value: String, url: String) {
        val existing = historyDao.getByUrl(key = KEY_BOOKMARK, url = url)
        if (existing != null) historyDao.delete(existing)
        historyDao.insertWithLimitCheck(
            HistoryEntity(
                id = 0,
                key = KEY_BOOKMARK,
                value = value,
                url = url
            )
        )
    }

    fun getBookmark(): Flow<List<HistoryEntity>> =
        historyDao.getByKey(KEY_BOOKMARK)

    /** ---- 浏览历史 ---- */
    suspend fun setBrowseHistory(value: String, url: String) {
        val existing = historyDao.getByUrl(key = KEY_BROWSE_HISTORY, url = url)
        if (existing != null) historyDao.delete(existing)
        historyDao.insertWithLimitCheck(
            HistoryEntity(
                id = 0,
                key = KEY_BROWSE_HISTORY,
                value = value,
                url = url
            )
        )
    }

    fun getBrowseHistory(): Flow<List<HistoryEntity>> =
        historyDao.getByKey(KEY_BROWSE_HISTORY)

    /** ---- 搜索历史 ---- */
    suspend fun setSearchHistory(value: String) {
        val existing = historyDao.getByValue(key = KEY_SEARCH_HISTORY, value = value)
        if (existing != null) historyDao.delete(existing)
        historyDao.insertWithLimitCheck(HistoryEntity(id = 0, key = KEY_SEARCH_HISTORY, value = value))
    }

    fun getSearchHistory(): Flow<List<HistoryEntity>> =
        historyDao.getByKey(KEY_SEARCH_HISTORY)

    /** ---- 通用删除 ---- */
    suspend fun deleteHistory(history: HistoryEntity) {
        historyDao.delete(history)
    }
}
