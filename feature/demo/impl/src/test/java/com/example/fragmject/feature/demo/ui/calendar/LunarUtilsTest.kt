package com.example.fragmject.feature.demo.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [LunarUtils] 的特征测试（characterization test）。
 *
 * 目的不是验证算法"最优"，而是在拆分这个 753 行文件之前，把**现有对外行为**钉死：
 * 拆分后这些断言必须原样通过，否则说明重构改变了语义。
 *
 * 日期取值来自公开的万年历事实，可直接人工核对：
 * - 2024-02-10 为甲辰年正月初一（春节）
 * - 2024-02-09 为癸卯年腊月三十
 * - 2023-03-22 为癸卯年闰二月初一（2023 年闰二月）
 */
class LunarUtilsTest {

    @Test
    fun `chinese new year 2024 is first day of first lunar month`() {
        val date = LunarUtils.getLunarDate(2024, 2, 10)

        assertEquals("甲辰年", date.lunarYear)
        assertEquals("正月", date.lunarMonth)
        assertEquals("初一", date.lunarDay)
        assertEquals("龙年", date.animalsYear)
        assertEquals("春节", date.lunarFestival)
        // getDay() 返回的是「公历年月日」拼接键（用于去重/缓存），不是农历日文案
        assertEquals("20240210", date.getDay())
        assertTrue(date.isFestival())
    }

    @Test
    fun `day before chinese new year 2024 is the last day of lunar year`() {
        val date = LunarUtils.getLunarDate(2024, 2, 9)

        assertEquals("腊月", date.lunarMonth)
        assertEquals("三十", date.lunarDay)
        // 腊月大月：除夕是三十
        assertEquals("除夕", date.lunarFestival)
    }

    @Test
    fun `new year eve falls on 29th when the twelfth month is short`() {
        // 2022 年春节是 2 月 1 日，前一天 1 月 31 日是腊月廿九（该年没有大年三十）
        val eve = LunarUtils.getLunarDate(2022, 1, 31)

        assertEquals("腊月", eve.lunarMonth)
        assertEquals("廿九", eve.lunarDay)
        assertEquals("除夕", eve.lunarFestival)

        val newYear = LunarUtils.getLunarDate(2022, 2, 1)
        assertEquals("春节", newYear.lunarFestival)
    }

    @Test
    fun `the 29th of a thirty-day twelfth month is not new year eve`() {
        // 2024 年腊月有三十天，廿九不应被误判为除夕
        val date = LunarUtils.getLunarDate(2024, 2, 8)

        assertEquals("腊月", date.lunarMonth)
        assertEquals("廿九", date.lunarDay)
        assertNull(date.lunarFestival)
        assertFalse(date.isFestival())
    }

    @Test
    fun `leap month is prefixed with run`() {
        // 2023 年闰二月，2023-03-22 为闰二月初一
        val date = LunarUtils.getLunarDate(2023, 3, 22)

        assertEquals("闰二月", date.lunarMonth)
        assertEquals("初一", date.lunarDay)
    }

    @Test
    fun `days of month respects leap year`() {
        assertEquals(29, LunarUtils.getDaysOfMonth(2024, 2))
        assertEquals(28, LunarUtils.getDaysOfMonth(2023, 2))
        assertEquals(31, LunarUtils.getDaysOfMonth(2024, 1))
        assertEquals(30, LunarUtils.getDaysOfMonth(2024, 4))
    }

    @Test
    fun `days between two dates`() {
        assertEquals(30, LunarUtils.getDaysOfTwoDate(2024, 1, 1, 2024, 1, 31))
        assertEquals(0, LunarUtils.getDaysOfTwoDate(2024, 1, 1, 2024, 1, 1))
        // 反向取负、跨年 +1
        assertEquals(-30, LunarUtils.getDaysOfTwoDate(2024, 1, 31, 2024, 1, 1))
        assertEquals(1, LunarUtils.getDaysOfTwoDate(2023, 12, 31, 2024, 1, 1))
        // 非法日期返回 0（旧实现依赖 SimpleDateFormat 的宽松解析，会把 2/30 变成 3/1）
        assertEquals(0, LunarUtils.getDaysOfTwoDate(2024, 2, 30, 2024, 3, 1))
    }

    @Test
    fun `constellation carries its date range`() {
        // 返回值形如「水瓶座:01.20—02.18」，UI 直接展示，故连区间一起锁住
        assertEquals("水瓶座:01.20—02.18", LunarUtils.getConstellation(2, 10))
        assertEquals("摩羯座:12.22—01.19", LunarUtils.getConstellation(1, 10))
    }

    @Test
    fun `dates before 1900 degrade to blank strings`() {
        val date = LunarUtils.getLunarDate(1899, 12, 31)

        assertEquals("", date.lunarYear)
        assertEquals("", date.lunarMonth)
        assertEquals("", date.lunarDay)
    }
}
