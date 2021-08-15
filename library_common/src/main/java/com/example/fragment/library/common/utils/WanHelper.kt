package com.example.fragment.library.common.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.fragment.library.base.bus.LiveDataBus
import com.example.fragment.library.base.utils.DBHelper
import com.example.fragment.library.common.bean.CoinBean
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.LiveBus
import com.example.fragment.library.common.bean.TreeBean
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
        DBHelper.set(UI_MODE, mode.toString())
    }

    /**
     * return
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun getUIMode(): LiveData<Int> {
        return Transformations.map(DBHelper.get(UI_MODE)) {
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
    fun setScreenRecordStatus(status: Int){
        DBHelper.set(SCREEN_RECORD, status.toString())
    }

    fun getScreenRecordStatus(): LiveData<Int> {
        return Transformations.map(DBHelper.get(SCREEN_RECORD)) {
            try {
                it.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    fun setUser(userBean: UserBean) {
        LiveDataBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).postEvent(userBean)
        DBHelper.set(USER, userBean.toJson())
    }

    fun getUser(): LiveData<UserBean> {
        return Transformations.map(DBHelper.get(USER)) {
            try {
                Gson().fromJson(it.toString(), UserBean::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                UserBean::class.java.newInstance()
            }
        }
    }

    fun setCoin(coinBean: CoinBean) {
        DBHelper.set(COIN, coinBean.toJson())
    }

    fun getCoin(): LiveData<CoinBean> {
        return Transformations.map(DBHelper.get(COIN)) {
            try {
                Gson().fromJson(it.toString(), CoinBean::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                CoinBean::class.java.newInstance()
            }
        }
    }

    fun setHotKey(hotKeys: List<HotKeyBean>) {
        DBHelper.set(HOT_KEY, Gson().toJson(hotKeys))
    }

    fun getHotKey(): LiveData<List<HotKeyBean>> {
        return Transformations.map(DBHelper.get(HOT_KEY)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<HotKeyBean>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

    fun setHistorySearch(list: List<String>) {
        DBHelper.set(HISTORY_SEARCH, Gson().toJson(list))
    }

    fun getHistorySearch(): LiveData<List<String>> {
        return Transformations.map(DBHelper.get(HISTORY_SEARCH)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

    fun setTreeList(list: List<TreeBean>) {
        DBHelper.set(TREE_LIST, Gson().toJson(list))
    }

    fun getTreeList(): LiveData<List<TreeBean>> {
        return Transformations.map(DBHelper.get(TREE_LIST)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<TreeBean>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

}