package com.example.fragment.library.common.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.base.db.KVDatabase
import com.example.fragment.library.common.bean.CoinBean
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.TreeBean
import com.example.fragment.library.common.bean.UserBean
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val UI_MODE = "ui_mode"
    private const val SCREEN_RECORD = "screen_record"
    private const val USER = "user"
    private const val COIN = "coin"
    private const val HOT_KEY = "hot_key"
    private const val HISTORY_SEARCH = "history_search"
    private const val TREE_LIST = "tree_list"

    /**
     * mode :
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun setUIMode(mode: Int) {
        KVDatabase.set(UI_MODE, mode.toString())
    }

    /**
     * return
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun getUIMode(): LiveData<Int> {
        return Transformations.map(KVDatabase.get(UI_MODE)) {
            try {
                it.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                1
            }
        }
    }

    /**
     * status :
     *       0 : 关闭,
     *       1 : 开启,
     */
    fun setScreenRecordStatus(status: Int) {
        KVDatabase.set(SCREEN_RECORD, status.toString())
    }

    fun getScreenRecordStatus(): LiveData<Int> {
        return Transformations.map(KVDatabase.get(SCREEN_RECORD)) {
            try {
                it.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    fun setUser(userBean: UserBean) {
        SharedFlowBus.with(UserBean::class.java).tryEmit(userBean)
        KVDatabase.set(USER, userBean.toJson())
    }

    fun getUser(): LiveData<UserBean> {
        return Transformations.map(KVDatabase.get(USER)) {
            try {
                Gson().fromJson(it.toString(), UserBean::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                UserBean()
            }
        }
    }

    fun setCoin(coinBean: CoinBean) {
        KVDatabase.set(COIN, coinBean.toJson())
    }

    fun getCoin(): LiveData<CoinBean> {
        return Transformations.map(KVDatabase.get(COIN)) {
            try {
                Gson().fromJson(it.toString(), CoinBean::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                CoinBean()
            }
        }
    }

    fun setHotKey(hotKeys: List<HotKeyBean>) {
        KVDatabase.set(HOT_KEY, Gson().toJson(hotKeys))
    }

    fun getHotKey(): LiveData<List<HotKeyBean>> {
        return Transformations.map(KVDatabase.get(HOT_KEY)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<HotKeyBean>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

    fun setHistorySearch(list: List<String>) {
        KVDatabase.set(HISTORY_SEARCH, Gson().toJson(list))
    }

    fun getHistorySearch(): LiveData<List<String>> {
        return Transformations.map(KVDatabase.get(HISTORY_SEARCH)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

    fun setTreeList(list: List<TreeBean>) {
        KVDatabase.set(TREE_LIST, Gson().toJson(list))
    }

    fun getTreeList(): LiveData<List<TreeBean>> {
        return Transformations.map(KVDatabase.get(TREE_LIST)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<TreeBean>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

}