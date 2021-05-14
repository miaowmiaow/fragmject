package com.example.fragment.library.common.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.fragment.library.base.bus.SimpleLiveBus
import com.example.fragment.library.base.db.SimpleDBHelper
import com.example.fragment.library.common.bean.CoinBean
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.LiveBus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WanHelper {

    private const val UI_MODE = "ui_mode"
    private const val USER = "user"
    private const val COIN = "coin"
    private const val HOT_KEY = "hot_key"
    private const val HISTORY_SEARCH = "history_search"

    /**
     * mode :
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun setUIMode(mode: Int) {
        SimpleDBHelper.set(UI_MODE, mode.toString())
    }

    /**
     * return
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun getUIMode(): LiveData<Int> {
        return Transformations.map(SimpleDBHelper.get(UI_MODE)) {
            try {
                it.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                -1
            }
        }
    }

    fun setUser(userBean: UserBean) {
        SimpleLiveBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).postEvent(userBean)
        SimpleDBHelper.set(USER, userBean.toJson())
    }

    fun getUser(): LiveData<UserBean> {
        return Transformations.map(SimpleDBHelper.get(USER)) {
            try {
                Gson().fromJson(it.toString(), UserBean::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                UserBean::class.java.newInstance()
            }
        }
    }

    fun setCoin(coinBean: CoinBean) {
        SimpleDBHelper.set(COIN, coinBean.toJson())
    }

    fun getCoin(): LiveData<CoinBean> {
        return Transformations.map(SimpleDBHelper.get(COIN)) {
            try {
                Gson().fromJson(it.toString(), CoinBean::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                CoinBean::class.java.newInstance()
            }
        }
    }

    fun setHotKey(hotKeys: List<HotKeyBean>) {
        SimpleDBHelper.set(HOT_KEY, Gson().toJson(hotKeys))
    }

    fun getHotKey(): LiveData<List<HotKeyBean>> {
        return Transformations.map(SimpleDBHelper.get(HOT_KEY)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<HotKeyBean>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

    fun setHistorySearch(list: List<String>) {
        SimpleDBHelper.set(HISTORY_SEARCH, Gson().toJson(list))
    }

    fun getHistorySearch(): LiveData<List<String>> {
        return Transformations.map(SimpleDBHelper.get(HISTORY_SEARCH)) {
            try {
                Gson().fromJson(it.toString(), object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                e.printStackTrace()
                ArrayList()
            }
        }
    }

}