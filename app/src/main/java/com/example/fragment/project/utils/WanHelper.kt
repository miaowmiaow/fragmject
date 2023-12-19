package com.example.fragment.project.utils

import android.util.Log
import com.example.fragment.project.data.User
import com.example.miaow.base.database.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 数据持久化辅助类
 */
object WanHelper {

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
    suspend fun getSearchHistory(): List<String> {
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

    suspend fun getWebBookmark(): List<String> {
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

    suspend fun getWebHistory(): List<String> {
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
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    suspend fun setUiMode(mode: Int): Boolean {
        return AppDatabase.set(UI_MODE, mode.toString())
    }

    /**
     * 显示模式状态
     * return
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    suspend fun getUiMode(): Int {
        return try {
            AppDatabase.get(UI_MODE).toInt()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            -1
        }
    }

    /**
     * 设置用户信息
     */
    suspend fun setUser(user: User?) {
        user?.let {
            AppDatabase.set(USER, it.toJson())
        }
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
        CoroutineScope(Dispatchers.IO).launch {
            val userBean = getUser()
            withContext(Dispatchers.Main) {
                result.invoke(userBean)
            }
        }
    }

    /**
     * 关闭数据库
     */
    fun close() {
        AppDatabase.closeDB()
    }

}