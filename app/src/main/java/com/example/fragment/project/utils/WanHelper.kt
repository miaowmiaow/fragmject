package com.example.fragment.project.utils

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.fragment.project.data.User
import com.example.miaow.base.database.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val SCHEDULE = "schedule"
    private const val SEARCH_HISTORY = "search_history"
    private const val WEB_BOOKMARK = "web_bookmark"
    private const val WEB_HISTORY = "web_history"
    private const val UI_MODE = "ui_mode"
    private const val USER = "user"

    /**
     * 设置搜索历史
     */
    suspend fun setSearchHistory(list: List<String>) {
        AppDatabase.set(SEARCH_HISTORY, Gson().toJson(list))
    }

    /**
     * 获取搜索历史
     */
    suspend fun getSearchHistory(): MutableList<String> {
        return try {
            val json = AppDatabase.get(SEARCH_HISTORY)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ArrayList()
        }
    }

    suspend fun setWebBookmark(list: List<String>) {
        AppDatabase.set(WEB_BOOKMARK, Gson().toJson(list))
    }

    suspend fun getWebBookmark(): MutableList<String> {
        return try {
            val json = AppDatabase.get(WEB_BOOKMARK)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            ArrayList()
        }
    }

    suspend fun setWebHistory(list: List<String>) {
        AppDatabase.set(WEB_HISTORY, Gson().toJson(list))
    }

    suspend fun getWebHistory(): MutableList<String> {
        return try {
            val json = AppDatabase.get(WEB_HISTORY)
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
        return AppDatabase.set(UI_MODE, mode.toString())
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
            AppDatabase.get(UI_MODE).toInt()
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

    /**
     * 设置用户信息
     */
    suspend fun setUser(user: User?) {
        if (user == null) {
            return
        }
        AppDatabase.set(USER, user.toJson())
    }

    /**
     * 获取用户信息
     */
    suspend fun getUser(): User {
        return try {
            Gson().fromJson(AppDatabase.get(USER), User::class.java) ?: User()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            User()
        }
    }

    fun getUser(result: (User) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            result.invoke(getUser())
        }
    }

    suspend fun setSchedule(year: Int, month: Int, day: Int, list: MutableList<String>) {
        AppDatabase.set("${SCHEDULE}_${year}_${month}_${day}", Gson().toJson(list))
    }

    suspend fun getSchedule(year: Int, month: Int, day: Int): MutableList<String> {
        return try {
            val json = AppDatabase.get("${SCHEDULE}_${year}_${month}_${day}")
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
    }

}