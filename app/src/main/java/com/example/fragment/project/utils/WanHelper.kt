package com.example.fragment.project.utils

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.project.database.AppDatabase
import com.example.fragment.project.database.user.User
import com.example.miaow.base.database.KVDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val SCHEDULE = "schedule"
    private const val SEARCH_HISTORY = "search_history"
    private const val WEB_BOOKMARK = "web_bookmark"
    private const val WEB_HISTORY = "web_history"
    private const val UI_MODE = "ui_mode"
    private const val USER_NAME = "user_name"

    /**
     * 设置用户信息
     */
    suspend fun setUser(user: User) {
        setUsername(user.username)
        val u = AppDatabase.getUserDao().getById(user.id)
        if (u == null) {
            AppDatabase.getUserDao().insert(user)
        } else {
            AppDatabase.getUserDao().update(user)
        }
    }

    /**
     * 删除用户信息
     */
    suspend fun deleteUser(user: User) {
        AppDatabase.getUserDao().delete(user)
        setUsername("")
    }

    /**
     * 获取用户信息
     */
    suspend fun getUser(): Flow<User?> {
        return withContext(Dispatchers.Main) {
            AppDatabase.getUserDao().getByName(getUsername())
        }
    }

    private suspend fun setUsername(username: String): Boolean {
        return KVDatabase.set(USER_NAME, username)
    }

    private suspend fun getUsername(): String {
        return KVDatabase.get(USER_NAME)
    }

    /**
     * 设置搜索历史
     */
    suspend fun setSearchHistory(list: List<String>) {
        val sublist = if (list.size > 1000) {
            list.takeLast(1000)
        } else {
            list
        }
        KVDatabase.set(SEARCH_HISTORY, Gson().toJson(sublist))
    }

    /**
     * 获取搜索历史
     */
    suspend fun getSearchHistory(): MutableList<String> {
        return try {
            val json = KVDatabase.get(SEARCH_HISTORY)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ArrayList()
        }
    }

    suspend fun setWebBookmark(list: List<String>) {
        val sublist = if (list.size > 1000) {
            list.takeLast(1000)
        } else {
            list
        }
        KVDatabase.set(WEB_BOOKMARK, Gson().toJson(sublist))
    }

    suspend fun getWebBookmark(): MutableList<String> {
        return try {
            val json = KVDatabase.get(WEB_BOOKMARK)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ArrayList()
        }
    }

    suspend fun setWebHistory(list: List<String>) {
        val sublist = if (list.size > 1000) {
            list.takeLast(1000)
        } else {
            list
        }
        KVDatabase.set(WEB_HISTORY, Gson().toJson(sublist))
    }

    suspend fun getWebHistory(): MutableList<String> {
        return try {
            val json = KVDatabase.get(WEB_HISTORY)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ArrayList()
        }
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