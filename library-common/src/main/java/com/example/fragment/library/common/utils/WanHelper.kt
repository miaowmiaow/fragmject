package com.example.fragment.library.common.utils

import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.base.db.KVDatabase
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.ProjectTreeBean
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
    private const val AVATAR = "avatar"
    private const val HOT_KEY = "hot_key"
    private const val PROJECT_TREES = "project_trees"
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

    /**
     * 设置用户信息
     */
    fun setUser(userBean: UserBean? = null) {
        if (userBean != null) {
            KVDatabase.set(USER, userBean.toJson())
            SharedFlowBus.withSticky(UserBean::class.java).tryEmit(userBean)
        }
    }

    /**
     * 获取用户信息
     */
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

    /**
     * 设置头像
     */
    fun setAvatar(path: String) {
        KVDatabase.set(AVATAR, path)
    }

    /**
     * 获取头像
     */
    fun getAvatar(result: (String) -> Unit) {
        KVDatabase.get(AVATAR) {
            result.invoke(it)
        }
    }

    /**
     * 设置热词
     */
    fun setHotKey(hotKeys: List<HotKeyBean>? = null) {
        if (hotKeys != null) {
            KVDatabase.set(HOT_KEY, Gson().toJson(hotKeys))
        }
    }

    /**
     * 获取热词
     */
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

    /**
     * 设置项目分类
     */
    fun setProjectTree(projectTrees: List<ProjectTreeBean>? = null) {
        if (projectTrees != null) {
            KVDatabase.set(PROJECT_TREES, Gson().toJson(projectTrees))
        }
    }

    /**
     * 获取项目分类
     */
    fun getProjectTree(result: (List<ProjectTreeBean>) -> Unit) {
        KVDatabase.get(PROJECT_TREES) {
            result.invoke(
                try {
                    Gson().fromJson(it, object : TypeToken<List<ProjectTreeBean>>() {}.type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ArrayList()
                }
            )
        }
    }

    /**
     * 设置搜索历史
     */
    fun setSearchHistory(list: List<String>) {
        KVDatabase.set(HISTORY_SEARCH, Gson().toJson(list))
    }

    /**
     * 获取搜索历史
     */
    fun getSearchHistory(result: (List<String>) -> Unit) {
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

    /**
     * 关闭数据库
     */
    fun close() {
        KVDatabase.closeDatabase()
    }

}