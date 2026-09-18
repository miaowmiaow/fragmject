package com.example.fragmject.feature.demo.ui.calendar

import androidx.lifecycle.ViewModel
import com.example.fragmject.core.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 日历示例页的 ViewModel：注入 [ScheduleRepository]，
 * 供 [Calendar] 组件通过领域端口读写日程。
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    val scheduleRepository: ScheduleRepository,
) : ViewModel()
