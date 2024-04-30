package com.example.fragment.project.components.calendar

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.ceil

data class CalendarUiState(
    val yearRange: IntRange = IntRange(1900, 2100),
    val dayNames: List<Pair<String, String>> = arrayListOf(),
    val monthMap: MutableMap<String, Month> = mutableMapOf(),
    val weekMappingMonth: MutableMap<Int, Pair<String, Int>> = mutableMapOf(),
    val monthMappingWeek: MutableMap<String, Int> = mutableMapOf(),
    var monthCount: Int = 0,
    var weekCount: Int = 0,
)

class CalendarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())

    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        val yearRange = IntRange(1900, 2100)
        val locale = Locale.CHINA
        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value
        val weekdays = with(locale) {
            DayOfWeek.entries.map {
                it.getDisplayName(TextStyle.FULL, this) to it.getDisplayName(TextStyle.NARROW, this)
            }
        }
        val dayNames = arrayListOf<Pair<String, String>>()
        for (i in firstDayOfWeek - 1 until weekdays.size) {
            dayNames.add(weekdays[i])
        }
        for (i in 0 until firstDayOfWeek - 1) {
            dayNames.add(weekdays[i])
        }
        var localDate = LocalDate.now()
        var monthCount = 0
        var weekCount = 0
        val monthMap = mutableMapOf<String, Month>()
        val weekMappingMonth = mutableMapOf<Int, Pair<String, Int>>()
        val monthMappingWeek = mutableMapOf<String, Int>()
        for (year in yearRange.first..yearRange.last) {
            for (month in 1..12) {
                localDate = localDate.withYear(year).withMonth(month).withDayOfMonth(1)
                val daysInMonth = localDate.lengthOfMonth()
                val difference = localDate.dayOfWeek.value - firstDayOfWeek
                val firstDayOfMonth = if (difference < 0) {
                    difference + CalendarDefaults.DAYS_IN_WEEK
                } else {
                    difference
                }
                val weeksInMonth =
                    ceil((daysInMonth + firstDayOfMonth).toDouble() / CalendarDefaults.DAYS_IN_WEEK).toInt()
                var cellIndex = 0
                val weeksData = mutableListOf<List<Date>>()
                for (week in 0 until weeksInMonth) {
                    val data = mutableListOf<Date>()
                    for (dayIndex in 0 until CalendarDefaults.DAYS_IN_WEEK) {
                        var y = year
                        var m = month
                        if (cellIndex < firstDayOfMonth) {
                            val lastMonth = localDate.plusMonths(-1)
                            val daysInLastMonth = lastMonth.lengthOfMonth()
                            val d = daysInLastMonth - (firstDayOfMonth - cellIndex) + 1
                            m -= 1
                            if (m < 1) {
                                y -= 1
                                m = 12
                            }
                            data.add(Date(y, m, d, false))
                        } else if (cellIndex >= (firstDayOfMonth + daysInMonth)) {
                            val d = cellIndex - (firstDayOfMonth + daysInMonth) + 1
                            m += 1
                            if (m > 12) {
                                y += 1
                                m = 1
                            }
                            data.add(Date(y, m, d, false))
                        } else {
                            val d = cellIndex - firstDayOfMonth + 1
                            data.add(Date(y, m, d, true))
                        }
                        cellIndex++
                    }
                    weeksData.add(data)
                    val index = weekCount++
                    weekMappingMonth[index] = Pair("${year}-${month}", week)
                    monthMappingWeek["${year}-${month}-${week}"] = index
                }
                val monthInfo = Month(year, month, weeksData)
                monthMap["${year}-${month}"] = monthInfo
                monthCount++
            }
        }
        _uiState.update {
            it.copy(
                yearRange = yearRange,
                dayNames = dayNames,
                monthMap = monthMap,
                weekMappingMonth = weekMappingMonth,
                monthMappingWeek = monthMappingWeek,
                monthCount = monthCount,
                weekCount = weekCount
            )
        }
    }

}