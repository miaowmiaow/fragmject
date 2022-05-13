package com.example.fragment.library.common.utils

import com.example.fragment.library.base.db.KVDatabase
import com.example.fragment.library.common.bean.UserBean
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val PRIVACY_AGREEMENT = "privacy_agreement"
    private const val SCREEN_RECORD = "screen_record"
    private const val SEARCH_HISTORY = "search_history"
    private const val UI_MODE = "ui_mode"
    private const val USER = "user"

    /**
     * 隐私协议状态
     */
    fun privacyAgreement(allow: () -> Unit, deny: () -> Unit) {
        KVDatabase.get(PRIVACY_AGREEMENT) {
            if (it == "1") allow.invoke() else deny.invoke()
        }
    }

    fun allowPrivacyAgreement() {
        KVDatabase.set(PRIVACY_AGREEMENT, "1")
    }

    fun denyPrivacyAgreement() {
        KVDatabase.set(PRIVACY_AGREEMENT, "0")
    }

    /**
     * 设置搜索历史
     */
    fun setSearchHistory(list: List<String>) {
        KVDatabase.set(SEARCH_HISTORY, Gson().toJson(list))
    }

    /**
     * 获取搜索历史
     */
    fun getSearchHistory(result: (List<String>) -> Unit) {
        KVDatabase.get(SEARCH_HISTORY) {
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
     * status :
     *       0 : 关闭,
     *       1 : 开启,
     */
    fun setScreenRecord(status: String) {
        KVDatabase.set(SCREEN_RECORD, status)
    }

    fun getScreenRecord(result: (String) -> Unit) {
        KVDatabase.get(SCREEN_RECORD) { result.invoke(it) }
    }

    /**
     * 设置用户信息
     */
    fun setUser(userBean: UserBean) {
        KVDatabase.set(USER, userBean.toJson())
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