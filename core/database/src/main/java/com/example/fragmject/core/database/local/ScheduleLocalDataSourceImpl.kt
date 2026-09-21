package com.example.fragmject.core.database.local

import android.util.Log
import com.example.fragmject.core.database.KVDatabase
import com.example.fragmject.core.database.model.JsonConverters
import com.example.fragmject.core.data.contract.local.ScheduleLocalDataSource
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ScheduleLocalDataSource] 的 KV 适配器实现。
 */
@Singleton
class ScheduleLocalDataSourceImpl @Inject constructor(
    private val kvDatabase: KVDatabase,
) : ScheduleLocalDataSource {

    private companion object {
        const val SCHEDULE_KEY_PREFIX = "schedule"
    }

    private val gson get() = JsonConverters.gson
    private val scheduleListType = object : TypeToken<List<String>>() {}.type

    override suspend fun setSchedule(year: Int, month: Int, day: Int, list: List<String>) {
        kvDatabase.setValue("${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}", gson.toJson(list))
    }

    override suspend fun getSchedule(year: Int, month: Int, day: Int): List<String> {
        return try {
            val json = kvDatabase.getValue("${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}")
            val parsed: List<String>? = gson.fromJson(json, scheduleListType)
            parsed?.let { ArrayList(it) } ?: ArrayList()
        } catch (e: Exception) {
            Log.e("ScheduleLocalDataSourceImpl", "getSchedule failed", e)
            ArrayList()
        }
    }
}
