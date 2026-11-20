package com.example.fragmject.core.database.local

import android.util.Log
import com.example.fragmject.core.database.KVDatabase
import com.example.fragmject.core.database.model.JsonConverters
import com.example.fragmject.core.domain.repository.ScheduleRepository
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ScheduleRepository] 的 KV 适配器实现。
 */
@Singleton
class ScheduleLocalDataSourceImpl @Inject constructor(
    private val kvDatabase: KVDatabase,
) : ScheduleRepository {

    private companion object {
        const val SCHEDULE_KEY_PREFIX = "schedule"
    }

    private val gson get() = JsonConverters.gson
    private val scheduleListType = object : TypeToken<List<String>>() {}.type

    override suspend fun setSchedule(year: Int, month: Int, day: Int, list: List<String>) {
        val key = "${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}"
        // KVDatabase 把 SQLite 异常吞成 false（内部已记 error）；本端口签名没有返回值，
        // 只能抛出来让上层感知。否则 ViewModel 的乐观更新不会回滚，界面显示成功但重启后丢失。
        if (!kvDatabase.setValue(key, gson.toJson(list))) {
            throw IOException("setSchedule failed: key=$key")
        }
    }

    override suspend fun getSchedule(year: Int, month: Int, day: Int): List<String> {
        return try {
            val json = kvDatabase.getValue("${SCHEDULE_KEY_PREFIX}_${year}_${month}_${day}")
            val parsed: List<String>? = gson.fromJson(json, scheduleListType)
            parsed?.let { ArrayList(it) } ?: ArrayList()
        } catch (e: JsonParseException) {
            Log.e("ScheduleLocalDataSourceImpl", "getSchedule failed", e)
            ArrayList()
        }
    }
}
