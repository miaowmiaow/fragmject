package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/** 日程面板的展示数据：由「选中日期 + 折叠模式 + 面板高度 + 当日条目」共同决定。 */
@Immutable
internal data class ScheduleContentSpec(
    val date: CalendarDate?,
    val mode: CalendarMode,
    val height: Dp,
    val schedule: List<String>,
)
