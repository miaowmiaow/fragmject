package com.example.fragment.library.common.utils

import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.base.db.KVDatabase
import com.example.fragment.library.common.bean.CoinBean
import com.example.fragment.library.common.bean.EventBean
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val UI_MODE = "ui_mode"
    private const val SCREEN_RECORD = "screen_record"
    private const val USER = "user"
    private const val AVATAR = "avatar"
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
        KVDatabase.set(UI_MODE, mode.toString())
    }

    /**
     * return
     *      -1 : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
     *       1 : AppCompatDelegate.MODE_NIGHT_NO,
     *       2 : AppCompatDelegate.MODE_NIGHT_YES
     */
    fun getUIMode(result: (Int) -> Unit) {
        KVDatabase.get(UI_MODE) {
            result.invoke(it.toInt())
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

    fun getScreenRecordStatus(result: (Int) -> Unit) {
        KVDatabase.get(SCREEN_RECORD) {
            result.invoke(it.toInt())
        }
    }

    fun setUser(userBean: UserBean? = null) {
        if (userBean != null) {
            KVDatabase.set(USER, userBean.toJson())
            SharedFlowBus.withSticky(UserBean::class.java).tryEmit(userBean)
        }
    }

    fun getUser(result: (UserBean) -> Unit) {
        KVDatabase.get(USER) {
            result.invoke(
                try {
                    Gson().fromJson(it, UserBean::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    UserBean()
                }
            )
        }
    }

    fun setAvatar(path: String) {
        KVDatabase.set(AVATAR, path)
        SharedFlowBus.withSticky(EventBean::class.java).tryEmit(EventBean(Keys.AVATAR, path))
    }

    fun getAvatar(result: (String) -> Unit) {
        KVDatabase.get(AVATAR) {
            result.invoke(it)
        }
    }

    fun setCoin(coinBean: CoinBean) {
        KVDatabase.set(COIN, coinBean.toJson())
    }

    fun getCoin(result: (CoinBean) -> Unit) {
        KVDatabase.get(COIN) {
            result.invoke(
                try {
                    Gson().fromJson(it, CoinBean::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    CoinBean()
                }
            )
        }
    }

    fun setHotKey(hotKeys: List<HotKeyBean>? = null) {
        if (hotKeys != null) {
            KVDatabase.set(HOT_KEY, Gson().toJson(hotKeys))
        }
    }

    fun getHotKey(result: (List<HotKeyBean>) -> Unit) {
        KVDatabase.get(HOT_KEY) {
            result.invoke(
                try {
                    Gson().fromJson(it, object : TypeToken<List<HotKeyBean>>() {}.type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ArrayList()
                }
            )
        }
    }

    fun setHistorySearch(list: List<String>) {
        KVDatabase.set(HISTORY_SEARCH, Gson().toJson(list))
    }

    fun getHistorySearch(result: (List<String>) -> Unit) {
        KVDatabase.get(HISTORY_SEARCH) {
            result.invoke(
                try {
                    Gson().fromJson(it, object : TypeToken<List<String>>() {}.type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ArrayList()
                }
            )
        }
    }

    fun close() {
        KVDatabase.closeDatabase()
    }

}