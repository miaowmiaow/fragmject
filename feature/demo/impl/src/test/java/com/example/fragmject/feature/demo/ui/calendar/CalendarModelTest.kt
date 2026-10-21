package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.material3.CalendarLocale
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `local date is inside the generated range`() {
        val date = model.localCalendarDate()
        assertNotNull(date)
        assertTrue(date!!.localDate.year in model.startYear()..model.lastYear())
        assertTrue(model.weekModeIndexByLocalDate() >= 0)
    }
}
