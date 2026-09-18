package com.example.fragmject.core.domain.repository

/**
 * 日程持久化领域端口。
 *
 * 供 core:ui 的日历组件通过领域接口访问日程数据，
 * 由 data 层实现（底层为 KV 存储），避免 UI 组件库直接依赖 database 层。
 */
interface ScheduleRepository {
    suspend fun setSchedule(year: Int, month: Int, day: Int, list: List<String>)
    suspend fun getSchedule(year: Int, month: Int, day: Int): List<String>
}
