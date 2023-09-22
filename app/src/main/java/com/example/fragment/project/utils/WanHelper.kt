package com.example.fragment.project.utils

import com.example.fragment.project.bean.UserBean
import com.example.miaow.base.db.KVDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val HISTORY_SEARCH = "history_search"
    private const val HISTORY_WEB = "history_web"
    private const val UI_MODE = "ui_mode"
    private const val USER = "user"

    /**
     * 设置搜索历史
     */
    fun setHistorySearch(list: List<String>) {
        KVDatabase.set(HISTORY_SEARCH, Gson().toJson(list))
    }

    /**
     * 获取搜索历史
     */
    fun getHistorySearch(result: (List<String>) -> Unit) {
        KVDatabase.get(HISTORY_SEARCH) {
            val list: List<String> = try {
                Gson().fromJson(it, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
            result.invoke(list)
        }
    }

    fun setHistoryWeb(list: List<String>) {
        KVDatabase.set(HISTORY_WEB, Gson().toJson(list))
    }

    fun getHistoryWeb(result: (List<String>) -> Unit) {
        KVDatabase.get(HISTORY_WEB) {
            val list: List<String> = try {
                Gson().fromJson(it, object : TypeToken<List<String>>() {}.type) ?: ArrayList()
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
            result.invoke(list)
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