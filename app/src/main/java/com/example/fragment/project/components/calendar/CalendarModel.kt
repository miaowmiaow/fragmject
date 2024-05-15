package com.example.fragment.project.components.calendar

import androidx.compose.material3.CalendarLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.math.ceil

class CalendarModel(locale: CalendarLocale) {

    var firstStartup: Boolean = true
    var localYear: Int = 0
    var localMonth: Int = 0
    var localDay: Int = 0
    var dayNames: MutableList<Pair<String, String>> = arrayListOf()
    private val calendarDateMap: MutableMap<String, CalendarDate> = mutableMapOf()
    private val calendarMonthMap: MutableMap<Int, CalendarMonth> = mutableMapOf()
    private val calendarWeekMap: MutableMap<Int, CalendarMonth> = mutableMapOf()
    private val monthModeDateMappingIndex: MutableMap<String, Int> = mutableMapOf()
    private val weekModeIndexMappingDate: MutableMap<Int, Pair<String, Int>> = mutableMapOf()
    private val weekModeDateMappingIndex: MutableMap<Pair<String, Int>, Int> = mutableMapOf()

    fun updateSchedule(schedules: List<CalendarSchedule>) {
        schedules.forEach {
            calendarDateMap["${it.year}-${it.month}-${it.day}"]?.setSchedule(it.schedules)
        }
    }

    init {
        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value
        val weekdays = with(locale) {
            DayOfWeek.entries.map {
                it.getDisplayName(TextStyle.FULL, this) to it.getDisplayName(TextStyle.NARROW, this)
            }
        }
        for (i in firstDayOfWeek - 1 until weekdays.size) {
            dayNames.add(weekdays[i])
        }
        for (i in 0 until firstDayOfWeek - 1) {
            dayNames.add(weekdays[i])
        }
        var localDate = LocalDate.now()
        localYear = localDate.year
        localMonth = localDate.monthValue
        localDay = localDate.dayOfMonth
        var monthCount = 0
        var weekCount = 0

        for (year in YEAR_RANGE.first..YEAR_RANGE.last) {
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
                val weeksData = mutableListOf<MutableList<CalendarDate>>()
                for (week in 0 until weeksInMonth) {
                    val data = mutableListOf<CalendarDate>()
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
                            data.add(CalendarDate(y, m, d))
                        } else if (cellIndex >= (firstDayOfMonth + daysInMonth)) {
                            val d = cellIndex - (firstDayOfMonth + daysInMonth) + 1
                            m += 1
                            if (m > 12) {
                                y += 1
                                m = 1
                            }
                            data.add(CalendarDate(y, m, d))
                        } else {
                            val d = cellIndex - firstDayOfMonth + 1
                            if (y == localYear && m == localMonth && d == localDay) {
                                selectedWeek = week
                                val date = CalendarDate(y, m, d, isMonth = true, isDay = true)
                                data.add(date)
                                calendarDateMap["${year}-${m}-${d}"] = date
                            } else {
                                val date = CalendarDate(y, m, d, true)
                                data.add(date)
                                calendarDateMap["${year}-${m}-${d}"] = date
                            }
                        }
                        cellIndex++
                    }
                    weeksData.add(data)
                    weekModeIndexMappingDate[weekCount] = Pair("${year}-${month}", week)
                    weekModeDateMappingIndex[Pair("${year}-${month}", week)] = weekCount
                    calendarWeekMap[weekCount] = CalendarMonth(year, month, weeksData).also {
                        it.selectedWeek = week
                    }
                    weekCount++
                }
                monthModeDateMappingIndex["${year}-${month}"] = monthCount
                calendarMonthMap[monthCount] = CalendarMonth(year, month, weeksData).also {
                    it.selectedWeek = selectedWeek
                }
                monthCount++
            }
        }
    }

    /**
     * 获取指定年月的日历日期对象
     */
    fun calendarDate(year: Int, month: Int, day: Int): CalendarDate {
        return calendarDateMap.getOrPut("${year}-${month}-${day}") {
            CalendarDate(year, month, day)
        }
    }

    fun monthModeCount(): Int {
        return calendarMonthMap.size
    }

    fun weekModeCount(): Int {
        return calendarWeekMap.size
    }

    fun monthModeByDate(year: Int, month: Int): CalendarMonth? {
        val key = monthModeDateMappingIndex["${year}-${month}"] ?: 0
        return calendarMonthMap[key]
    }

    /**
     * 根据索引获取月模式的日历月份
     */
    fun monthModeByIndex(index: Int): CalendarMonth? {
        return calendarMonthMap[index]
    }

    /**
     * 根据索引获取周模式的日历月份
     */
    fun weekModeByIndex(index: Int): CalendarMonth? {
        return calendarWeekMap[index]
    }

    /**
     * 根据日期获取周模式的日历月份
     */
    fun weekModeIndexByDate(year: Int, month: Int): Int {
        val key = monthModeDateMappingIndex["${year}-${month}"] ?: 0
        val week = calendarMonthMap[key]?.selectedWeek ?: 0
        return weekModeDateMappingIndex[Pair("${year}-${month}", week)] ?: 0
    }

    fun yearByWeekModeIndex(index: Int): Int {
        val pair = weekModeIndexMappingDate[index] ?: return startYear()
        return pair.first.split("-")[0].toInt()
    }

    fun monthByWeekModeIndex(index: Int): Int {
        val pair = weekModeIndexMappingDate[index] ?: return 0
        return pair.first.split("-")[1].toInt()
    }

    fun weekByWeekModeIndex(index: Int): Int {
        val pair = weekModeIndexMappingDate[index] ?: return 0
        return pair.second
    }

    fun startYear(): Int {
        return YEAR_RANGE.first
    }

    fun lastYear(): Int {
        return YEAR_RANGE.last
    }
}

internal val YEAR_RANGE = IntRange(1900, 2100)
internal const val DAYS_IN_WEEK = 7

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val weeks: MutableList<MutableList<CalendarDate>>,
    var selectedWeek: Int = 0,
) {
    fun weeksInMonth(): Int {
        return weeks.size
    }
}

data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int = 1,
    val isMonth: Boolean = false,
    var isDay: Boolean = false,
) {

    private var schedule: MutableList<String> = mutableListOf()

    fun setSchedule(schedule: List<String>?) {
        schedule?.let {
            this.schedule.clear()
            this.schedule.addAll(it)
        }
    }

    fun getSchedule(): MutableList<String> {
        return schedule
    }

    private var lunarDate: LunarDate? = null

    private fun lunar(): LunarDate {
        return lunarDate ?: getLunarDate(year, month, day).also { lunarDate = it }
    }

    fun animalsYear(): String {
        return lunar().animalsYear
    }

    fun lunarYear(): String {
        return lunar().lunarYear
    }

    fun lunarMonth(): String {
        return lunar().lunarMonth
    }

    fun lunarDay(): String {
        return lunar().lunarDay
    }

    fun getFestival(): List<String> {
        return lunar().getFestival()
    }

    fun getFirstFestival(): String {
        return lunar().getFirstFestival()
    }

    fun isFestival(): Boolean {
        return lunar().isFestival()
    }
}

data class CalendarSchedule(
    val year: Int,
    val month: Int,
    val day: Int,
    val schedules: MutableList<String>,
)