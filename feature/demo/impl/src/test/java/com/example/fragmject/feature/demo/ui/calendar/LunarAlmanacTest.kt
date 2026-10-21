package com.example.fragmject.feature.demo.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 农历人文展示：生肖、农历日文案、星座边界、节气与宜忌。
 *
 * 只断言稳定的口径（边界与格式），不锁死逐年文案，避免运营调整导致测试噪音。
 */
class LunarAlmanacTest {

    @Test
    fun `animal year follows the twelve year cycle`() {
        assertEquals("鼠年", LunarAlmanac.animalsYear(2020))
        assertEquals("牛年", LunarAlmanac.animalsYear(2021))
        assertEquals("龙年", LunarAlmanac.animalsYear(2024))
    }

    @Test
    fun `chinese day handles single tens and the thirtieth`() {
        assertEquals("初一", LunarAlmanac.getChinaDayString(1))
        assertEquals("初十", LunarAlmanac.getChinaDayString(10))
        assertEquals("十一", LunarAlmanac.getChinaDayString(11))
        assertEquals("二十", LunarAlmanac.getChinaDayString(20))
        assertEquals("廿一", LunarAlmanac.getChinaDayString(21))
        assertEquals("三十", LunarAlmanac.getChinaDayString(30))
    }

    @Test
    fun `constellation boundaries fall on the expected side`() {
        assertEquals(LunarTables.CONSTELLATIONS[1], LunarAlmanac.getConstellation(1, 20))
        assertEquals(LunarTables.CONSTELLATIONS[0], LunarAlmanac.getConstellation(1, 19))
        assertEquals(LunarTables.CONSTELLATIONS[3], LunarAlmanac.getConstellation(3, 21))
        assertEquals(LunarTables.CONSTELLATIONS[11], LunarAlmanac.getConstellation(12, 21))
        assertEquals(LunarTables.CONSTELLATIONS[0], LunarAlmanac.getConstellation(12, 22))
    }

    @Test
    fun `solar term day stays inside the month`() {
        for (year in listOf(1900, 2000, 2024, 2100)) {
            for (term in 0..23) {
                val day = LunarAlmanac.solarTerm(year, term)
                assertTrue("year=$year term=$term day=$day", day in 1..31)
            }
        }

        val february = LunarAlmanac.getDateOfSolarTerms(2024, 2)
        assertTrue("立春与雨水应按 MMDD 组合", february in 101..1231)
    }

    @Test
    fun `cyclical year pillar is two characters`() {
        val pillar = LunarAlmanac.cyclical(2024, 3, 1)
        assertEquals(2, pillar.length)
        assertTrue(LunarTables.GAN.any { pillar.startsWith(it) })
    }

    @Test
    fun `yi ji text covers all twelve day officers`() {
        val officers = listOf("建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭")
        officers.forEach { officer ->
            val text = LunarAlmanac.jcrt(officer)
            assertTrue("officer=$officer", text.startsWith("宜:"))
            assertTrue("officer=$officer", text.contains("忌:"))
        }
        assertEquals("", LunarAlmanac.jcrt("未知"))
    }

    @Test
    fun `taboo combos report year break month break and yang gong`() {
        assertEquals("日值岁破 大事不宜", LunarAlmanac.calConv2(0, 0, 6, 0, 0, 0, 1))
        assertEquals("日值月破 大事不宜", LunarAlmanac.calConv2(2, 0, 6, 0, 0, 0, 1))
        assertEquals("日值杨公十三忌 大事不宜", LunarAlmanac.calConv2(2, 2, 2, 0, 0, 1, 13))
        assertNull(LunarAlmanac.calConv2(2, 2, 2, 0, 0, 2, 2))
    }

    @Test
    fun `daily yi ji is produced for every day of the month`() {
        val month = LunarAlmanac.getYiJi(2024, 2)
        assertEquals(LunarUtils.getDaysOfMonth(2024, 2), month.size)
        month.forEach { assertTrue(it.isNullOrBlank().not()) }

        val day = LunarAlmanac.getYiJi(2024, 2, 10)
        assertTrue(day.startsWith("宜:"))
    }
}
