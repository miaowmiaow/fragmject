package com.example.fragment.project.utils

import android.util.Log
import com.example.fragment.project.database.AppDatabase
import com.example.fragment.project.data.History
import com.example.fragment.project.data.User
import com.example.miaow.base.database.KVDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val BOOKMARK = "bookmark"
    private const val BROWSE_HISTORY = "browse_history"
    private const val SCHEDULE = "schedule"
    private const val SEARCH_HISTORY = "search_history"

    suspend fun setBookmark(value: String, url: String) {
        val historyDao = AppDatabase.getHistoryDao()
        val history = historyDao.getByUrl(key = BOOKMARK, url = url)
        if (history != null) {
            historyDao.delete(history)
        }
        historyDao.insertWithLimitCheck(History(key = BOOKMARK, value = value, url = url))
    }

    fun getBookmark(): Flow<List<History>> {
        return AppDatabase.getHistoryDao().getByKey(BOOKMARK)
    }

    suspend fun setBrowseHistory(value: String, url: String) {
        val historyDao = AppDatabase.getHistoryDao()
        val history = historyDao.getByUrl(key = BROWSE_HISTORY, url = url)
        if (history != null) {
            historyDao.delete(history)
        }
        historyDao.insertWithLimitCheck(History(key = BROWSE_HISTORY, value = value, url = url))
    }

    fun getBrowseHistory(): Flow<List<History>> {
        return AppDatabase.getHistoryDao().getByKey(BROWSE_HISTORY)
    }

    /**
     * 设置搜索历史
     */
    suspend fun setSearchHistory(value: String) {
        val historyDao = AppDatabase.getHistoryDao()
        val history = historyDao.getByValue(key = SEARCH_HISTORY, value = value)
        if (history != null) {
            historyDao.delete(history)
        }
        historyDao.insertWithLimitCheck(History(key = SEARCH_HISTORY, value = value))
    }

    /**
     * 获取搜索历史
     */
    fun getSearchHistory(): Flow<List<History>> {
        return AppDatabase.getHistoryDao().getByKey(SEARCH_HISTORY)
    }

    suspend fun deleteHistory(history: History) {
        AppDatabase.getHistoryDao().delete(history)
    }

    /**
     * 设置用户信息
     */
    suspend fun setUser(user: User) {
        AppDatabase.getUserDao().insert(user)
    }

    /**
     * 删除用户信息
     */
    suspend fun deleteUser(user: User) {
        AppDatabase.getUserDao().delete(user)
    }

    /**
     * 获取用户信息
     */
    fun getUser(): Flow<User?> {
        return AppDatabase.getUserDao().get()
    }

    suspend fun setSchedule(year: Int, month: Int, day: Int, list: MutableList<String>) {
        KVDatabase.set("${SCHEDULE}_${year}_${month}_${day}", Gson().toJson(list))
    }

    suspend fun getSchedule(year: Int, month: Int, day: Int): MutableList<String> {
        return try {
            val json = KVDatabase.get("${SCHEDULE}_${year}_${month}_${day}")
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ArrayList()
        }
    }

    /**
     * 关闭数据库
     */
    fun close() {
        AppDatabase.closeDB()
        KVDatabase.closeDB()
    }

}