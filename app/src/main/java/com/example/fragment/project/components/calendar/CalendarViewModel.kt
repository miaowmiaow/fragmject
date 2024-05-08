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
    val dayNames: List<Pair<String, String>> = arrayListOf(),
    private val monthMap: MutableMap<String, Month> = mutableMapOf(),
    private val weekIndexMappingDate: MutableMap<Int, Pair<String, Int>> = mutableMapOf(),
    private val dateMappingWeekIndex: MutableMap<String, Int> = mutableMapOf(),
    var monthCount: Int = 0,
    var weekCount: Int = 0,
) {
    fun monthByDate(year: Int, month: Int): Month? {
        return monthMap["${year}-${month}"]
    }

    fun weekIndexByDate(year: Int, month: Int, week: Int): Int {
        return dateMappingWeekIndex["${year}-${month}-${week}"] ?: 0
    }

    fun yearByWeekIndex(index: Int): Int {
        val pair = weekIndexMappingDate[index] ?: return startYear()
        return pair.first.split("-")[0].toInt()
    }

    fun monthByWeekIndex(index: Int): Int {
        val pair = weekIndexMappingDate[index] ?: return 0
        return pair.first.split("-")[1].toInt()
    }

    fun weekByWeekIndex(index: Int): Int {
        val pair = weekIndexMappingDate[index] ?: return 0
        return pair.second
    }

    fun startYear(): Int {
        return CalendarViewModel.YearRange.first
    }

    fun lastYear(): Int {
        return CalendarViewModel.YearRange.last
    }
}

class CalendarViewModel : ViewModel() {

    companion object {
        val YearRange: IntRange = IntRange(1900, 2100)
        const val DAYS_IN_WEEK = 7
    }

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
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
        val selectedDay = localDate.dayOfMonth
        var monthCount = 0
        var weekCount = 0
        val monthMap = mutableMapOf<String, Month>()
        val weekIndexMappingDate = mutableMapOf<Int, Pair<String, Int>>()
        val dateMappingWeekIndex = mutableMapOf<String, Int>()
        for (year in YearRange.first..YearRange.last) {
            for (month in 1..12) {
                localDate = localDate.withYear(year).withMonth(month).withDayOfMonth(1)
                val daysInMonth = localDate.lengthOfMonth()
                val difference = localDate.dayOfWeek.value - firstDayOfWeek
                val firstDayOfMonth = if (difference < 0) {
                    difference + DAYS_IN_WEEK
                } else {
                    difference
                }
                val weeksInMonth =
                    ceil((daysInMonth + firstDayOfMonth).toDouble() / DAYS_IN_WEEK).toInt()
                var cellIndex = 0
                var selectedWeek = 0
                val weeksData = mutableListOf<List<Date>>()
                for (week in 0 until weeksInMonth) {
                    val data = mutableListOf<Date>()
                    for (dayIndex in 0 until DAYS_IN_WEEK) {
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
                            if (d == selectedDay) {
                                selectedWeek = week
                            }
                            data.add(Date(y, m, d, true))
                        }
                        cellIndex++
                    }
                    weeksData.add(data)
                    val index = weekCount++
                    weekIndexMappingDate[index] = Pair("${year}-${month}", week)
                    dateMappingWeekIndex["${year}-${month}-${week}"] = index
                }
                val monthInfo = Month(year, month, weeksData)
                monthInfo.selectedWeek = selectedWeek
                monthMap["${year}-${month}"] = monthInfo
                monthCount++
            }
        }
        _uiState.update {
            it.copy(
                dayNames = dayNames,
                monthMap = monthMap,
                weekIndexMappingDate = weekIndexMappingDate,
                dateMappingWeekIndex = dateMappingWeekIndex,
                monthCount = monthCount,
                weekCount = weekCount
            )
        }
    }
}

data class Month(
    val year: Int,
    val month: Int,
    val weeks: MutableList<List<Date>>,
    var selectedWeek: Int = 0,
) {
    fun weeksInMonth(): Int {
        return weeks.size
    }
}

data class Date(
    val year: Int,
    val month: Int,
    val day: Int,
    val isMonth: Boolean,
) {
    fun year(): String {
        return year.toString()
    }

    fun month(): String {
        return month.toString().padStart(2, '0')
    }

    fun day(): String {
        return day.toString().padStart(2, '0')
    }

    fun lunar(): LunarDate {
        return getLunarDate(year, month, day)
    }
}