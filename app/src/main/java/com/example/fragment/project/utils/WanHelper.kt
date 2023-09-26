package com.example.fragment.project.utils

import com.example.fragment.project.bean.UserBean
import com.example.miaow.base.db.KVDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val SEARCH_HISTORY = "search_history"
    private const val WEB_COLLECT = "web_collect"
    private const val WEB_BROWSE = "web_browse"
    private const val UI_MODE = "ui_mode"
    private const val USER = "user"

    /**
     * 设置搜索历史
     */
    fun setSearchHistory(list: List<String>) {
        KVDatabase.set(SEARCH_HISTORY, Gson().toJson(list))
    }

    /**
     * 获取搜索历史
     */
    suspend fun getSearchHistory(): List<String> {
        return try {
            val json = KVDatabase.get(SEARCH_HISTORY)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            e.printStackTrace()
            ArrayList()
        }
    }

    fun setWebBrowse(list: List<String>) {
        KVDatabase.set(WEB_BROWSE, Gson().toJson(list))
    }

    suspend fun getWebBrowse(): List<String> {
        return try {
            val json = KVDatabase.get(WEB_BROWSE)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            e.printStackTrace()
            ArrayList()
        }
    }

    fun setWebCollect(list: List<String>) {
        KVDatabase.set(WEB_COLLECT, Gson().toJson(list))
    }

    suspend fun getWebCollect(): List<String> {
        return try {
            val json = KVDatabase.get(WEB_COLLECT)
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
        } catch (e: Exception) {
            e.printStackTrace()
            ArrayList()
        }
    }

    /**
     * mode :
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun setUiMode(mode: String) {
        KVDatabase.set(UI_MODE, mode)
    }

    /**
     * 显示模式状态
     * return
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun getUiMode(result: (String) -> Unit) {
        KVDatabase.get(UI_MODE) {
            result.invoke(it)
        }
    }

    /**
     * 设置用户信息
     */
    fun setUser(userBean: UserBean?) {
        userBean?.let {
            KVDatabase.set(USER, it.toJson())
        }
    }

    /**
     * 获取用户信息
     */
    fun getUser(result: (UserBean) -> Unit) {
        KVDatabase.get(USER) {
            val userBean = try {
                Gson().fromJson(it, UserBean::class.java) ?: UserBean()
            } catch (e: Exception) {
                e.printStackTrace()
                UserBean()
            }
            result.invoke(userBean)
        }
    }

    /**
     * 关闭数据库
     */
    fun close() {
        KVDatabase.closeDatabase()
    }

}