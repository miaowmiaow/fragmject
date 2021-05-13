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

    private const val USER = "user"
    private const val COIN = "coin"
    private const val HOT_KEY = "hot_key"
    private const val HISTORY_SEARCH = "history_search"

    fun setUser(userBean: UserBean) {
        SimpleDBHelper.set(USER, userBean.toJson())
        SimpleLiveBus.with<Boolean>(LiveBus.USER_STATUS_UPDATE).postEvent(true)
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