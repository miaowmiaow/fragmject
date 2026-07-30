package com.example.fragmject.core.database.store

import android.util.Log
import com.example.fragmject.core.database.KVDatabase
import com.example.fragmject.core.network.utils.GSonUtils
import com.google.gson.reflect.TypeToken

/**
 * 日程持久化存储。
 *
 * 原置于 :app 的 WanHelper 中，现移至 core:db，
 * 使 calendar 组件可独立引用日程存储，无需依赖 :app。
 *
 * 使用 KVDatabase 作为底层存储，不依赖 Room。
 */
object ScheduleStore {

    private const val SCHEDULE_KEY_PREFIX = "schedule"

    private val gson get() = GSonUtils.gson
    private val scheduleListType = object : TypeToken<List<String>>() {}.type

    suspend fun setSchedule(year: Int, month: Int, day: Int, list: MutableList<String>) {
        KVDatabase.set("${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}", gson.toJson(list))
    }

    suspend fun getSchedule(year: Int, month: Int, day: Int): MutableList<String> {
        return try {
            val json = KVDatabase.get("${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}")
            val parsed: List<String>? = gson.fromJson(json, scheduleListType)
            parsed?.let { ArrayList(it) } ?: ArrayList()
        } catch (e: Exception) {
            Log.e("ScheduleStore", "getSchedule failed", e)
            ArrayList()
        }
    }
}
