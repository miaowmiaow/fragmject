package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.material3.CalendarLocale
import java.time.LocalDate
import java.time.temporal.WeekFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 日历模型：1900–2100 的月/周两种索引必须自洽，且能反查任意公历日。
 */
class CalendarModelTest {

    private val model = CalendarModel(CalendarLocale("en-US"))

    @Test
    fun `year range and mode counts are consistent`() {
        assertEquals(1900, model.startYear())
        assertEquals(2100, model.lastYear())
        assertEquals(201 * 12, model.monthModeCount())
        assertTrue(model.weekModeCount() > model.monthModeCount())
    }

    @Test
    fun `month index round trips back to the same month`() {
        val first = model.monthModeByIndex(0)
        assertNotNull(first)
        assertEquals(1900, first!!.year)
        assertEquals(1, first.month)
        assertTrue(first.weeksInMonth() in 4..6)

        val last = model.monthModeByIndex(model.monthModeCount() - 1)
        assertNotNull(last)
        assertEquals(2100, last!!.year)
        assertEquals(12, last.month)
    }

    @Test
    fun `calendar date lookup returns the day with lunar info`() {
        val date = model.calendarDate(LocalDate.of(2024, 2, 10))
        assertNotNull(date)
        assertEquals(2024, date!!.year)
        assertEquals(2, date.month)
        assertEquals(10, date.day)
        assertEquals("2024-02-10", date.localDate.toString())
        // 2024-02-10 为农历正月初一
        assertEquals("初一", date.lunarDay())
        assertTrue(date.getFirstFestival().isNotEmpty() || date.isFestival().not())
    }

    @Test
    fun `week mode index maps back to year month and week`() {
        val index = model.weekModeIndexByDate(2024, 2, 1)
        assertEquals(2024, model.yearByWeekModeIndex(index))
        assertEquals(2, model.monthByWeekModeIndex(index))
        assertEquals(1, model.weekByWeekModeIndex(index))

        val month = model.weekModeByIndex(index)
        assertNotNull(month)
        assertEquals(2024, month!!.year)
        assertEquals(2, month.month)
    }

    @Test
    fun `year-based offsets match every month across the supported range`() {
        val firstDayOfWeek = WeekFields.of(CalendarLocale("en-US")).firstDayOfWeek.value
        var expectedOffset = 0
        for (year in model.startYear()..model.lastYear()) {
            for (month in 1..12) {
                val firstDate = LocalDate.of(year, month, 1)
                val firstDayOffset =
                    (firstDate.dayOfWeek.value - firstDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
                val weekCount =
                    (firstDate.lengthOfMonth() + firstDayOffset + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
                val lastWeekIndex = expectedOffset + weekCount - 1

                assertEquals(expectedOffset, model.weekModeIndexByDate(year, month))
                assertEquals(year, model.yearByWeekModeIndex(lastWeekIndex))
                assertEquals(month, model.monthByWeekModeIndex(lastWeekIndex))
                assertEquals(weekCount - 1, model.weekByWeekModeIndex(lastWeekIndex))
                expectedOffset += weekCount
            }
        }
        assertEquals(expectedOffset, model.weekModeCount())
    }

    @Test
    fun `construction does not prebuild any month`() {
        val fresh = CalendarModel(CalendarLocale("en-US"))
        assertEquals("构造只应算索引，不应生成月份数据", 0, fresh.cachedMonthCount())

        assertNotNull(fresh.monthModeByIndex(0))
        assertEquals(1, fresh.cachedMonthCount())

        // 同一索引返回缓存实例，重复访问不再重建
        fresh.monthModeByIndex(0)
        assertEquals(1, fresh.cachedMonthCount())
    }

    @Test
    fun `month cache stays bounded and evicts least recently used entries`() {
        val fresh = CalendarModel(CalendarLocale("en-US"))
        val firstMonth = fresh.monthModeByIndex(0)

        for (index in 1..MAX_CACHED_MONTHS) {
            fresh.monthModeByIndex(index)
        }

        assertEquals(MAX_CACHED_MONTHS, fresh.cachedMonthCount())
        val rebuiltFirstMonth = fresh.monthModeByIndex(0)
        assertNotNull(rebuiltFirstMonth)
        assertTrue(firstMonth !== rebuiltFirstMonth)
        assertEquals(MAX_CACHED_MONTHS, fresh.cachedMonthCount())
    }

    @Test
    fun `week mode index is consistent at the last month and out of range`() {
        val lastIndex = model.weekModeCount() - 1
        val month = model.weekModeByIndex(lastIndex)
        assertNotNull(month)
        assertEquals(2100, month!!.year)
        assertEquals(12, month.month)
        assertEquals(
            lastIndex,
            model.weekModeIndexByDate(2100, 12, month.weeksInMonth() - 1),
        )

        assertNull(model.weekModeByIndex(-1))
        assertNull(model.weekModeByIndex(model.weekModeCount()))
        assertNull(model.monthModeByIndex(model.monthModeCount()))
    }

    @Test
    fun `local date is inside the generated range`() {
        val date = model.localCalendarDate()
        assertNotNull(date)
        assertTrue(date!!.localDate.year in model.startYear()..model.lastYear())
        assertTrue(model.weekModeIndexByLocalDate() >= 0)
    }
}
