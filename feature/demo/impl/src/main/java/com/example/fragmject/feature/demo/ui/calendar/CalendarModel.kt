package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.material3.CalendarLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * 日历数据模型：1900–2100 年的月/周两种索引。
 *
 * 构造时**不生成任何月份数据**；每月周数由 14 种年份类型模板计算，只按年累计周页偏移。
 * 整月的周格与 [CalendarDate] 推迟到首次访问该月时才构建并缓存：
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

    /** 按「1 月 1 日星期 × 是否闰年」缓存一年内各月的周数，避免逐月创建日期对象。 */
    private val weekCountsByYearType: Array<IntArray> = Array(YEAR_TYPE_COUNT) { type ->
        var firstDayOfMonth = type / 2 + 1
        val leapYear = type % 2 == 1
        IntArray(MONTHS_PER_YEAR) { monthIndex ->
            val daysInMonth = daysInMonth(monthIndex + 1, leapYear)
            val firstDayOffset = (firstDayOfMonth - firstDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
            val weeks = (daysInMonth + firstDayOffset + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
            firstDayOfMonth = (firstDayOfMonth + daysInMonth - 1) % DAYS_IN_WEEK + 1
            weeks
        }
    }

    private val weeksPerYearType: IntArray = IntArray(YEAR_TYPE_COUNT) { type ->
        weekCountsByYearType[type].sum()
    }

    /** 每年的周模式起始页码，长度为年数 + 1，末位即周模式总页数。 */
    private val yearWeekOffsets: IntArray = IntArray(YEAR_RANGE.count() + 1)

    /** 各年对应的「1 月 1 日星期 × 是否闰年」类型。 */
    private val yearTypes: ByteArray = ByteArray(YEAR_RANGE.count())

    /**
     * 最近访问的月份（月模式索引 → 月份数据）；按访问顺序淘汰，限制长期浏览的内存占用。
     *
     * 只在主线程访问（组合阶段与 ViewModel 的翻页同步都在主线程）。
     */
    private val monthCache = LinkedHashMap<Int, CalendarMonth>(MAX_CACHED_MONTHS, 0.75f, true)

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
        for ((yearIndex, year) in YEAR_RANGE.withIndex()) {
            val firstDayOfYear = LocalDate.of(year, 1, 1).dayOfWeek.value
            val isLeapYear = year.isLeapYear()
            val yearType = (firstDayOfYear - 1) * 2 + if (isLeapYear) 1 else 0
            yearTypes[yearIndex] = yearType.toByte()
            yearWeekOffsets[yearIndex] = offset
            offset += weeksPerYearType[yearType]
        }
        yearWeekOffsets[yearTypes.size] = offset
    }

    private fun Int.isLeapYear(): Boolean = this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)

    private fun daysInMonth(month: Int, leapYear: Boolean): Int = when (month) {
        2 -> if (leapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    private fun weeksInMonth(monthIndex: Int): Int {
        val yearIndex = monthIndex / MONTHS_PER_YEAR
        val monthOfYear = monthIndex % MONTHS_PER_YEAR
        return weekCountsByYearType[yearTypes[yearIndex].toInt()][monthOfYear]
    }

    private fun weekOffsetForMonth(monthIndex: Int): Int {
        val yearIndex = monthIndex / MONTHS_PER_YEAR
        val monthOfYear = monthIndex % MONTHS_PER_YEAR
        val weeksByMonth = weekCountsByYearType[yearTypes[yearIndex].toInt()]
        var offset = yearWeekOffsets[yearIndex]
        for (month in 0 until monthOfYear) {
            offset += weeksByMonth[month]
        }
        return offset
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
        val weeksInMonth = weeksInMonth(index)
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
        return yearWeekOffsets[yearTypes.size]
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
        monthCache[index]?.let { return it }

        val year = YEAR_RANGE.first + index / MONTHS_PER_YEAR
        val month = index % MONTHS_PER_YEAR + 1
        val calendarMonth = buildMonth(index, year, month)
        monthCache[index] = calendarMonth
        if (monthCache.size > MAX_CACHED_MONTHS) {
            monthCache.remove(monthCache.entries.iterator().next().key)
        }
        return calendarMonth
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
        if (week !in 0 until weeksInMonth(index)) return 0
        return weekOffsetForMonth(index) + week
    }

    fun weekModeIndexByLocalDate(): Int {
        val week = calendarDate(localYear, localMonth, localDay)?.week ?: 0
        return weekModeIndexByDate(localYear, localMonth, week)
    }

    /** 当前保留在 LRU 缓存中的月份数，供测试验证惰性构建与缓存上限。 */
    internal fun cachedMonthCount(): Int = monthCache.size

    /** 周模式页码 → 月索引：先定位年份，再在该年最多 12 个月内查找。 */
    private fun monthIndexByWeekModeIndex(index: Int): Int? {
        if (index < 0 || index >= weekModeCount()) return null
        val position = yearWeekOffsets.binarySearch(index, 0, yearTypes.size)
        val yearIndex = if (position >= 0) position else -position - 2
        val weeksByMonth = weekCountsByYearType[yearTypes[yearIndex].toInt()]
        var weekInYear = index - yearWeekOffsets[yearIndex]
        val firstMonthIndex = yearIndex * MONTHS_PER_YEAR
        for (month in weeksByMonth.indices) {
            if (weekInYear < weeksByMonth[month]) return firstMonthIndex + month
            weekInYear -= weeksByMonth[month]
        }
        return null
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
        return index - weekOffsetForMonth(monthIndex)
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
internal const val MAX_CACHED_MONTHS = 36
private const val YEAR_TYPE_COUNT = DAYS_IN_WEEK * 2

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
