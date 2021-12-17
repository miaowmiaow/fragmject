package com.example.fragment.library.common.utils

import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.base.db.KVDatabase
import com.example.fragment.library.common.bean.EventBean
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.ProjectTreeBean
import com.example.fragment.library.common.bean.UserBean
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据持久化辅助类
 */
object WanHelper {

    private const val HOT_KEY = "hot_key"
    private const val LOCAL_AVATAR = "local_avatar"
    private const val PROJECT_TREES = "project_trees"
    const val SCREEN_RECORD = "screen_record"
    private const val SEARCH_HISTORY = "search_history"
    const val UI_MODE = "ui_mode"
    const val USER = "user"

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
     * 设置本地头像
     */
    fun setLocalAvatar(path: String) {
        KVDatabase.set(LOCAL_AVATAR, path)
    }

    /**
     * 获取本地头像
     */
    fun getLocalAvatar(result: (String) -> Unit) {
        KVDatabase.get(LOCAL_AVATAR) {
            result.invoke(it)
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
     * status :
     *       0 : 关闭,
     *       1 : 开启,
     */
    fun setScreenRecord(status: String) {
        KVDatabase.set(SCREEN_RECORD, status)
        SharedFlowBus.withSticky(EventBean::class.java).tryEmit(EventBean(SCREEN_RECORD, status))
    }

    fun getScreenRecord() {
        KVDatabase.get(SCREEN_RECORD) {
            SharedFlowBus.withSticky(EventBean::class.java).tryEmit(EventBean(SCREEN_RECORD, it))
        }
    }

    fun registerScreenRecord(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<EventBean>) {
        SharedFlowBus.onSticky(EventBean::class.java).observe(owner, observer)
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

    fun registerUIMode(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<EventBean>) {
        SharedFlowBus.onSticky(EventBean::class.java).observe(owner, observer)
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
    fun getUser() {
        KVDatabase.get(USER) {
            SharedFlowBus.withSticky(UserBean::class.java).tryEmit(
                try {
                    Gson().fromJson(it, UserBean::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    UserBean()
                }
            )
        }
    }

    fun registerUser(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<UserBean>) {
        SharedFlowBus.onSticky(UserBean::class.java).observe(owner, observer)
    }

    /**
     * 关闭数据库
     */
    fun close() {
        KVDatabase.closeDatabase()
    }

}