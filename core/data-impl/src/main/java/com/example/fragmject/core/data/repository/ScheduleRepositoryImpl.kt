package com.example.fragmject.core.data.impl.repository

import com.example.fragmject.core.data.contract.local.ScheduleLocalDataSource
import com.example.fragmject.core.domain.repository.ScheduleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程持久化 data 实现，依赖 data-contract 的 [ScheduleLocalDataSource]（底层 KV 存储）。
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val local: ScheduleLocalDataSource,
) : ScheduleRepository {

    override suspend fun setSchedule(year: Int, month: Int, day: Int, list: List<String>) {
        local.setSchedule(year, month, day, list)
    }

    override suspend fun getSchedule(year: Int, month: Int, day: Int): List<String> {
        return local.getSchedule(year, month, day)
    }
}
