package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.material3.CalendarLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.math.ceil

/**
 * 日历数据模型：1900–2100 年的月/周两种索引。
 *
 * 构造时**不生成任何月份数据**，只按 locale 算好星期名、今天，以及每月的周数
 * （纯整数数组）。整月的周格与 [CalendarDate] 推迟到首次访问该月时才构建并缓存：
 *
 * 旧实现在 `remember(defaultLocale)` 里同步遍历 201×12 个月、构造约 9 万个
 * [CalendarDate]（每个还带一个 [LocalDate]），首帧与 locale 切换都要在主线程付这笔钱；
 * 而一屏最多只用到相邻两三个月，绝大多数对象永远不会被显示。
 */
class CalendarModel(
    locale: CalendarLocale,
) {

    var firstStartup: Boolean = true
    var dayNames: MutableList<Pair<String, String>> = arrayListOf()
    var localYear: Int = 0
    var localMonth: Int = 0
    private var localDay: Int = 0

    /** 当前 locale 一周的第一天（1 = 周一 … 7 = 周日）。 */
    private val firstDayOfWeek: Int = WeekFields.of(locale).firstDayOfWeek.value

    private val monthCount: Int = YEAR_RANGE.count() * MONTHS_PER_YEAR

    /**
     * 每月的周数（含首尾补齐格），只有整数。
     *
     * 周模式的总页数与「(年月, 周) → 页码」换算都依赖它，所以必须预先算好；
     * 但它只涉及每月一次的日历算术，不产生任何日期对象。
     */
    private val weeksPerMonth: IntArray = IntArray(monthCount)

    /** 每月周模式的起始页码，长度 [monthCount] + 1，末位即周模式总页数。 */
    private val weekOffsets: IntArray = IntArray(monthCount + 1)

    /**
     * 已构建的月份（月模式索引 → 月份数据）。
     *
     * 只在主线程访问（组合阶段与 ViewModel 的翻页同步都在主线程），故用普通 HashMap。
     */
    private val monthCache: HashMap<Int, CalendarMonth> = HashMap()

    init {
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

        val today = LocalDate.now()
        localYear = today.year
        localMonth = today.monthValue
        localDay = today.dayOfMonth

        var offset = 0
        var index = 0
        for (year in YEAR_RANGE) {
            for (month in 1..MONTHS_PER_YEAR) {
                val firstDate = LocalDate.of(year, month, 1)
                val firstDayOfMonth =
                    (firstDate.dayOfWeek.value - firstDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
                val weeks = ceil(
                    (firstDate.lengthOfMonth() + firstDayOfMonth).toDouble() / DAYS_IN_WEEK,
                ).toInt()
                weeksPerMonth[index] = weeks
                weekOffsets[index] = offset
                offset += weeks
                index++
            }
        }
        weekOffsets[monthCount] = offset
    }

    private fun adjustYearAndMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        var y = year
        var m = month + delta
        if (m < 1) {
            y -= 1
            m += 12
        } else if (m > 12) {
            y += 1
            m -= 12
        }
        return Pair(y, m)
    }

    /** 月模式下「年月 → 月索引」；超出 [YEAR_RANGE] 返回 null。 */
    private fun monthIndex(year: Int, month: Int): Int? {
        if (year !in YEAR_RANGE || month !in 1..MONTHS_PER_YEAR) return null
        return (year - YEAR_RANGE.first) * MONTHS_PER_YEAR + (month - 1)
    }

    /** 按需构建某个月的周格与日期对象，结果由 [monthCache] 缓存。 */
    private fun buildMonth(index: Int, year: Int, month: Int): CalendarMonth {
        val firstDateOfMonth = LocalDate.of(year, month, 1)
        val firstDayOfMonth =
            (firstDateOfMonth.dayOfWeek.value - firstDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
        val daysInMonth = firstDateOfMonth.lengthOfMonth()
        val daysInLastMonth = firstDateOfMonth.minusMonths(1).lengthOfMonth()
        val weeksInMonth = weeksPerMonth[index]
        val weeksData = ArrayList<MutableList<CalendarDate>>(weeksInMonth)
        val daysData = ArrayList<CalendarDate>(daysInMonth)
        var cellIndex = 0
        for (week in 0 until weeksInMonth) {
            val data = ArrayList<CalendarDate>(DAYS_IN_WEEK)
            repeat(DAYS_IN_WEEK) {
                val date = when {
                    // 上月补齐格
                    cellIndex < firstDayOfMonth -> {
                        val d = daysInLastMonth - (firstDayOfMonth - cellIndex) + 1
                        val (y, m) = adjustYearAndMonth(year, month, -1)
                        CalendarDate(y, m, d, week)
                    }
                    // 下月补齐格
                    cellIndex >= (firstDayOfMonth + daysInMonth) -> {
                        val d = cellIndex - (firstDayOfMonth + daysInMonth) + 1
                        val (y, m) = adjustYearAndMonth(year, month, 1)
                        CalendarDate(y, m, d, week)
                    }
                    else -> {
                        val d = cellIndex - firstDayOfMonth + 1
                        CalendarDate(year, month, d, week, true).also(daysData::add)
                    }
                }
                data.add(date)
                cellIndex++
            }
            weeksData.add(data)
        }
        return CalendarMonth(year, month, weeksData, daysData)
    }

    /**
     * 获取指定年月的日历日期对象
     */
    private fun calendarDate(year: Int, month: Int, day: Int): CalendarDate? {
        return monthModeByDate(year, month)?.days?.getOrNull(day - 1)
    }

    /** 根据 [LocalDate] 反查日历日期对象，供组件按选中日期获取展示信息。 */
    fun calendarDate(date: LocalDate): CalendarDate? {
        return monthModeByDate(date.year, date.monthValue)?.days?.getOrNull(date.dayOfMonth - 1)
    }

    fun localCalendarDate(): CalendarDate? {
        return monthModeByDate(localYear, localMonth)?.days?.getOrNull(localDay - 1)
    }

    fun monthModeCount(): Int {
        return monthCount
    }

    fun weekModeCount(): Int {
        return weekOffsets[monthCount]
    }

    private fun monthModeByDate(year: Int, month: Int): CalendarMonth? {
        val index = monthIndex(year, month) ?: return null
        return monthModeByIndex(index)
    }

    /**
     * 根据索引获取月模式的日历月份。
     *
     * 该月的周格与日期对象在首次访问时才构建；越界返回 null。
     */
    fun monthModeByIndex(index: Int): CalendarMonth? {
        if (index !in 0 until monthCount) return null
        return monthCache.getOrPut(index) {
            val year = YEAR_RANGE.first + index / MONTHS_PER_YEAR
            val month = index % MONTHS_PER_YEAR + 1
            buildMonth(index, year, month)
        }
    }

    /**
     * 根据索引获取周模式的日历月份；越界返回 null。
     */
    fun weekModeByIndex(index: Int): CalendarMonth? {
        val monthIndex = monthIndexByWeekModeIndex(index) ?: return null
        return monthModeByIndex(monthIndex)
    }

    /**
     * 根据日期获取周模式的页码；年月或周序号越界时返回 0。
     */
    fun weekModeIndexByDate(year: Int, month: Int, week: Int = 0): Int {
        val index = monthIndex(year, month) ?: return 0
        if (week !in 0 until weeksPerMonth[index]) return 0
        return weekOffsets[index] + week
    }

    fun weekModeIndexByLocalDate(): Int {
        val week = calendarDate(localYear, localMonth, localDay)?.week ?: 0
        return weekModeIndexByDate(localYear, localMonth, week)
    }

    /** 已构建的月份数；仅供测试验证「构造时不预生成全量月份」。 */
    internal fun builtMonthCount(): Int = monthCache.size

    /** 周模式页码 → 月索引（[weekOffsets] 上做二分）；越界返回 null。 */
    private fun monthIndexByWeekModeIndex(index: Int): Int? {
        if (index < 0 || index >= weekModeCount()) return null
        val position = weekOffsets.binarySearch(index, 0, monthCount)
        // 命中即该月起点；未命中时插入点的前一位才是「最后一个起始页 <= index」的月份
        return if (position >= 0) position else -position - 2
    }

    fun yearByWeekModeIndex(index: Int): Int {
        val monthIndex = monthIndexByWeekModeIndex(index) ?: return startYear()
        return YEAR_RANGE.first + monthIndex / MONTHS_PER_YEAR
    }

    fun monthByWeekModeIndex(index: Int): Int {
        val monthIndex = monthIndexByWeekModeIndex(index) ?: return 0
        return monthIndex % MONTHS_PER_YEAR + 1
    }

    fun weekByWeekModeIndex(index: Int): Int {
        val monthIndex = monthIndexByWeekModeIndex(index) ?: return 0
        return index - weekOffsets[monthIndex]
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
internal const val MONTHS_PER_YEAR = 12

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val weeks: MutableList<MutableList<CalendarDate>>,
    val days: MutableList<CalendarDate>,
) {
    fun weeksInMonth(): Int {
        return weeks.size
    }
}

data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int = 1,
    val week: Int,
    val currMonth: Boolean = false,
) {

    /** 日期唯一标识，供页面级状态持有者按日索引日程与选中态。 */
    val localDate: LocalDate = LocalDate.of(year, month, day)

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
