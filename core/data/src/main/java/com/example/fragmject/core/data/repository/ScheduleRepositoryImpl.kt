package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.store.ScheduleStore
import com.example.fragmject.core.domain.repository.ScheduleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程持久化 data 实现，委托 [ScheduleStore]（底层 KV 存储）。
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleStore: ScheduleStore,
) : ScheduleRepository {

    override suspend fun setSchedule(year: Int, month: Int, day: Int, list: List<String>) {
        scheduleStore.setSchedule(year, month, day, list.toMutableList())
    }

    override suspend fun getSchedule(year: Int, month: Int, day: Int): List<String> {
        return scheduleStore.getSchedule(year, month, day)
    }
}
