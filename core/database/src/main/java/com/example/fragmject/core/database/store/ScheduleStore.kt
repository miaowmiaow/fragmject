package com.example.fragmject.core.database.store

import android.util.Log
import com.example.fragmject.core.database.KVDatabase
import com.example.fragmject.core.common.utils.GsonUtils
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程持久化存储。
 *
 * 使用 KVDatabase 作为底层存储，不依赖 Room。
 */
@Singleton
class ScheduleStore @Inject constructor(
    private val kvDatabase: KVDatabase,
) {

    private companion object {
        const val SCHEDULE_KEY_PREFIX = "schedule"
    }

    private val gson get() = GsonUtils.gson
    private val scheduleListType = object : TypeToken<List<String>>() {}.type

    suspend fun setSchedule(year: Int, month: Int, day: Int, list: MutableList<String>) {
        kvDatabase.setValue("${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}", gson.toJson(list))
    }

    suspend fun getSchedule(year: Int, month: Int, day: Int): MutableList<String> {
        return try {
            val json = kvDatabase.getValue("${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}")
            val parsed: List<String>? = gson.fromJson(json, scheduleListType)
            parsed?.let { ArrayList(it) } ?: ArrayList()
        } catch (e: Exception) {
            Log.e("ScheduleStore", "getSchedule failed", e)
            ArrayList()
        }
    }
}
