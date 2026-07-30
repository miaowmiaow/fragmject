package com.example.fragmject.core.database.store

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.model.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书签 / 历史 / 搜索记录持久化存储 facade。
 *
 * 直接封装 [com.example.fragmject.core.database.AppDatabase] 的 HistoryDao，按 key 划分数据域。
 * ViewModel 可直接注入或引用本对象，无需经过 WanHelper。
 */
object HistoryStore {

    private const val KEY_BOOKMARK = "bookmark"
    private const val KEY_BROWSE_HISTORY = "browse_history"
    private const val KEY_SEARCH_HISTORY = "search_history"

    /** ---- 书签 ---- */
    suspend fun setBookmark(value: String, url: String) {
        val dao = AppDatabase.getHistoryDao()
        val existing = dao.getByUrl(key = KEY_BOOKMARK, url = url)
        if (existing != null) dao.delete(existing)
        dao.insertWithLimitCheck(HistoryEntity(id = 0, key = KEY_BOOKMARK, value = value, url = url))
    }

    fun getBookmark(): Flow<List<HistoryEntity>> = AppDatabase.getHistoryDao().getByKey(KEY_BOOKMARK)

    /** ---- 浏览历史 ---- */
    suspend fun setBrowseHistory(value: String, url: String) {
        val dao = AppDatabase.getHistoryDao()
        val existing = dao.getByUrl(key = KEY_BROWSE_HISTORY, url = url)
        if (existing != null) dao.delete(existing)
        dao.insertWithLimitCheck(HistoryEntity(id = 0, key = KEY_BROWSE_HISTORY, value = value, url = url))
    }

    fun getBrowseHistory(): Flow<List<HistoryEntity>> = AppDatabase.getHistoryDao().getByKey(KEY_BROWSE_HISTORY)

    /** ---- 搜索历史 ---- */
    suspend fun setSearchHistory(value: String) {
        val dao = AppDatabase.getHistoryDao()
        val existing = dao.getByValue(key = KEY_SEARCH_HISTORY, value = value)
        if (existing != null) dao.delete(existing)
        dao.insertWithLimitCheck(HistoryEntity(id = 0, key = KEY_SEARCH_HISTORY, value = value))
    }

    fun getSearchHistory(): Flow<List<HistoryEntity>> = AppDatabase.getHistoryDao().getByKey(KEY_SEARCH_HISTORY)

    /** ---- 通用删除 ---- */
    suspend fun deleteHistory(history: HistoryEntity) {
        AppDatabase.getHistoryDao().delete(history)
    }
}
