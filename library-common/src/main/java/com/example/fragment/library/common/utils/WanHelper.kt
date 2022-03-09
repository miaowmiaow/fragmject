package com.example.fragment.library.common.utils

import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.base.db.KVDatabase
import com.example.fragment.library.common.bean.EventBean
import com.example.fragment.library.common.bean.UserBean
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val SCREEN_RECORD = "screen_record"
    private const val SEARCH_HISTORY = "search_history"
    const val UI_MODE = "ui_mode"
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
    fun setUIMode(mode: String) {
        KVDatabase.set(UI_MODE, mode)
        SharedFlowBus.withSticky(EventBean::class.java).tryEmit(EventBean(UI_MODE, mode))
    }

    /**
     * 显示模式状态
     * return
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun getUIMode() {
        KVDatabase.get(UI_MODE) {
            SharedFlowBus.withSticky(EventBean::class.java).tryEmit(EventBean(UI_MODE, it))
        }
    }

    /**
     * 监听显示模式状态
     */
    fun registerUIMode(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<EventBean>) {
        SharedFlowBus.onSticky(EventBean::class.java).observe(owner, observer)
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