package com.example.fragment.project.utils

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.project.database.AppDatabase
import com.example.fragment.project.database.history.History
import com.example.fragment.project.database.user.User
import com.example.miaow.base.database.KVDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val BOOKMARK = "bookmark"
    private const val BROWSE_HISTORY = "browse_history"
    private const val SCHEDULE = "schedule"
    private const val SEARCH_HISTORY = "search_history"
    private const val UI_MODE = "ui_mode"

    suspend fun setBookmark(url: String) {
        val history = AppDatabase.getHistoryDao().getByValue(key = BOOKMARK, value = url)
        if (history != null) {
            AppDatabase.getHistoryDao().delete(history)
        }
        AppDatabase.getHistoryDao().insert(History(key = BOOKMARK, value = url))
    }

    fun getBookmark(): Flow<List<History>> {
        return AppDatabase.getHistoryDao().getByType(BOOKMARK)
    }

    suspend fun setBrowseHistory(url: String) {
        val history = AppDatabase.getHistoryDao().getByValue(key = BROWSE_HISTORY, value = url)
        if (history != null) {
            AppDatabase.getHistoryDao().delete(history)
        }
        AppDatabase.getHistoryDao().insert(History(key = BROWSE_HISTORY, value = url))
    }

    fun getBrowseHistory(): Flow<List<History>> {
        return AppDatabase.getHistoryDao().getByType(BROWSE_HISTORY)
    }

    /**
     * 设置搜索历史
     */
    suspend fun setSearchHistory(title: String) {
        val history = AppDatabase.getHistoryDao().getByValue(key = SEARCH_HISTORY, value = title)
        if (history != null) {
            AppDatabase.getHistoryDao().delete(history)
        }
        AppDatabase.getHistoryDao().insert(History(key = SEARCH_HISTORY, value = title))
    }

    /**
     * 获取搜索历史
     */
    fun getSearchHistory(): Flow<List<History>> {
        return AppDatabase.getHistoryDao().getByType(SEARCH_HISTORY)
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

    /**
     * mode :
     *      AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *      AppCompatDelegate.MODE_NIGHT_NO,
     *      AppCompatDelegate.MODE_NIGHT_YES
     */
    suspend fun setUiMode(mode: Int): Boolean {
        return KVDatabase.set(UI_MODE, mode.toString())
    }

    /**
     * 显示模式状态
     * return
     *       AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       AppCompatDelegate.MODE_NIGHT_NO,
     *       AppCompatDelegate.MODE_NIGHT_YES
     */
    suspend fun getUiMode(): Int {
        return try {
            KVDatabase.get(UI_MODE).toInt()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    fun getUiMode(result: (Int) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            result.invoke(getUiMode())
        }
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