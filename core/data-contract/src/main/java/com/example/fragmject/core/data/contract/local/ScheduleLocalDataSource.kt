package com.example.fragmject.core.data.contract.local

/**
 * 日程本地数据源。
 *
 * 由 core:database 使用 KV 存储实现；core:data 只依赖本契约。
 */
interface ScheduleLocalDataSource {
    suspend fun setSchedule(year: Int, month: Int, day: Int, list: List<String>)
    suspend fun getSchedule(year: Int, month: Int, day: Int): List<String>
}
