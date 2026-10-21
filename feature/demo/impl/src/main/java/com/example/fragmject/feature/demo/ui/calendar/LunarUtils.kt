package com.example.fragmject.feature.demo.ui.calendar

import android.util.Log
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Calendar

fun getLunarDate(year: Int, month: Int, day: Int): LunarDate {
    return LunarUtils.getLunarDate(year, month, day)
}

fun getDaysOfTwoDate(bY: Int, bM: Int, bD: Int, nY: Int, nM: Int, nD: Int): Int {
    return LunarUtils.getDaysOfTwoDate(bY, bM, bD, nY, nM, nD)
}

/**
 * 中国农历工具类。
 *
 * 无状态纯函数集合，直接使用 [object] 作为命名空间：天然线程安全，
 * 替代原先 `class + 双检锁 INSTANCE` 的单例写法。
 */
object LunarUtils {


    // ====== 传回农历 y年的总天数 1900--2100
    fun animalsYear(year: Int): String = LunarAlmanac.animalsYear(year)
    fun getConstellation(month: Int, day: Int): String = LunarAlmanac.getConstellation(month, day)
    fun getDateOfSolarTerms(year: Int, month: Int): Int = LunarAlmanac.getDateOfSolarTerms(year, month)
    fun jcrt(d: String): String = LunarAlmanac.jcrt(d)
    fun calConv2(yy: Int, mm: Int, dd: Int, y: Int, d: Int, m: Int, dt: Int): String? = LunarAlmanac.calConv2(yy, mm, dd, y, d, m, dt)
    fun getYiJi(year: Int, month: Int): ArrayList<String?> = LunarAlmanac.getYiJi(year, month)
    fun getYiJi(year: Int, month: Int, day: Int): String = LunarAlmanac.getYiJi(year, month, day)

    private fun yearDays(y: Int): Int {
        var i: Int
        var sum = 348
        i = 0x8000
        while (i > 0x8) {
            if ((LunarTables.LUNAR_INFO[y - 1900] and i.toLong()) != 0L) sum += 1
            i = i shr 1
        }
        return (sum + leapDays(y))
    }

    // ====== 传回农历 y年闰月的天数
    internal fun leapDays(y: Int): Int {
        return if (leapMonth(y) != 0) {
            if ((LunarTables.LUNAR_INFO[y - 1899] and 0xfL) != 0L) 30
            else 29
        } else 0
    }

    // ====== 传回农历 y年闰哪个月 1-12 , 没闰传回 0
    internal fun leapMonth(y: Int): Int {
        val lun = LunarTables.LUNAR_INFO[y - 1900] and 0xfL
        return (if (lun == 0xfL) 0 else lun).toInt()
    }

    // ====== 传回农历 y年m月的总天数
    internal fun monthDays(y: Int, m: Int): Int {
        return if ((LunarTables.LUNAR_INFO[y - 1900] and (0x10000 shr m).toLong()) == 0L) 29
        else 30
    }

    /**
     * 传回两个公历日期相差的天数（[bY]-[bM]-[bD] 到 [nY]-[nM]-[nD]，后者更早则为负）。
     *
     * 用 [LocalDate] 而不是共享的 `SimpleDateFormat`：
     * - `SimpleDateFormat` 不是线程安全的，放在 object 里被并发调用会解析出错；
     * - 按「毫秒差 / 一天」计算会受夏令时影响（DST 切换当天是 23 或 25 小时），跨 DST 会差 1 天；
     *   [LocalDate.toEpochDay] 是纯日历日序，与时区/夏令时无关。
     *
     * 任一日期非法（如 2 月 30 日）时返回 0，保持调用方「算不出来就不换算」的既有约定。
     */
    fun getDaysOfTwoDate(bY: Int, bM: Int, bD: Int, nY: Int, nM: Int, nD: Int): Int {
        return try {
            val base = LocalDate.of(bY, bM, bD)
            val now = LocalDate.of(nY, nM, nD)
            (now.toEpochDay() - base.toEpochDay()).toInt()
        } catch (e: DateTimeException) {
            Log.e("LunarUtils", "getDaysOfTwoDate invalid date", e)
            0
        }
    }

    /* 农历lunYear年lunMonth月lunDay日
     * isLeap 当前年月是否是闰月
     * 从农历日期转换成公历日期
     * */
    fun getSunDate(lYear: Int, lMonth: Int, lDay: Int, isLeap: Boolean): Calendar {
        //公历1900年1月31日为1900年正月初一
        val years = lYear - 1900
        var days = 0
        for (i in 0 until years) {
            days += yearDays(1900 + i) //农历某年总天数
        }
        for (i in 1 until lMonth) {
            days += monthDays(lYear, i)
        }
        if (leapMonth(lYear) != 0 && lMonth > leapMonth(lYear)) {
            days += leapDays(lYear) //lYear年闰月天数
        }
        if (isLeap) {
            days += monthDays(lYear, lMonth) //lunYear年lunMonth月 闰月
        }
        days += lDay
        days -= 1
        val cal = Calendar.getInstance()
        cal[Calendar.YEAR] = 1900
        cal[Calendar.MONTH] = 0
        cal[Calendar.DAY_OF_MONTH] = 31
        cal.add(Calendar.DATE, days)
        /*
	    Date date=cal.getTime();
	    int year_c = cal.get(Calendar.YEAR);
	    int month_c = cal.get(Calendar.MONTH)+1;
	    int day_c = cal.get(Calendar.DAY_OF_MONTH);
	    */
        return cal
    }

    /*
     * 将公历year年month月day日转换成农历
     * 返回格式为20140506（int）
     * */
    fun getLunarDateInt(year: Int, month: Int, day: Int): Int {
        // lDateInt 编码：yyyymmdd，如 20240315
        val (lYear, offsetInYear) = resolveLunarYear(year, month, day)
        val (lMonth, lDay) = resolveLunarMonthDay(lYear, offsetInYear)
        return lYear * 10000 + lMonth * 100 + lDay
    }

    /**
     * 从 1900-01-31 起逐年扣减农历年天数，得到农历年份与当年剩余天数。
     */
    private fun resolveLunarYear(year: Int, month: Int, day: Int): Pair<Int, Int> {
        // 与 1900 年 1 月 31 日相差的天数
        var offset = getDaysOfTwoDate(1900, 1, 31, year, month, day)
        var daysOfYear = 0
        var iYear = 1900
        while (iYear < 2100 && offset > 0) {
            daysOfYear = yearDays(iYear)
            offset -= daysOfYear
            iYear++
        }
        // 多减了一年（跨年边界）需要退回
        if (offset < 0) {
            offset += daysOfYear
            iYear--
        }
        return iYear to offset
    }

    /**
     * 用当年剩余天数逐月扣减，得到农历月与日；闰月按 [leapMonth] 插入并做两次边界校正。
     */
    private fun resolveLunarMonthDay(lYear: Int, offsetInYear: Int): Pair<Int, Int> {
        var offset = offsetInYear
        val leapMonth = leapMonth(lYear) // 闰哪个月,1-12
        var leap = false
        var iMonth = 1
        var daysOfMonth = 0
        while (iMonth < 13 && offset > 0) {
            if (leapMonth > 0 && iMonth == (leapMonth + 1) && !leap) {
                // 进入闰月：重复上一个月的序号
                --iMonth
                leap = true
                daysOfMonth = leapDays(lYear)
            } else {
                daysOfMonth = monthDays(lYear, iMonth)
            }
            // 解除闰月
            if (leap && iMonth == (leapMonth + 1)) leap = false

            offset -= daysOfMonth
            iMonth++
        }
        // offset 为 0 且刚好落在闰月：校正月份归属
        if (offset == 0 && leapMonth > 0 && iMonth == leapMonth + 1) {
            if (leap) {
                leap = false
            } else {
                leap = true
                --iMonth
            }
        }
        // offset 小于 0：回退到上一个月
        if (offset < 0) {
            offset += daysOfMonth
            --iMonth
        }
        return iMonth to (offset + 1)
    }


    fun getLunarString(year: Int, month: Int, day: Int): String {
        val lDateInt = getLunarDateInt(year, month, day)
        val lYear = lDateInt / 10000
        val lMonth = (lDateInt % 10000) / 100
        val lDay = lDateInt - lYear * 10000 - lMonth * 100
        val lY = LunarAlmanac.cyclical(year, month, day) + "年"
        var lM = ""
        val testMonth = getSunDate(lYear, lMonth, lDay, false)[Calendar.MONTH] + 1
        if (testMonth != month) {
            lM = "闰"
        }
        lM += LunarTables.CHINESE_NUMBER[(lMonth - 1) % 12] + "月"
        //int leap = leapMonth(lYear);
        val lD = LunarAlmanac.getChinaDayString(lDay)
        var animalsYear = 0
        val term2 = LunarAlmanac.solarTerm(year, 2) //立春
        animalsYear = if (month > 2 || (month == 2 && day >= term2)) {
            year
        } else {
            year - 1
        }
        return lY + " " + animalsYear(animalsYear) + " " + lM + lD
    }

    /** 农历换算的支撑区间：1900-01-31 起、2100 年止，区间外不换算。 */
    private fun isOutOfSupportedRange(year: Int, month: Int, day: Int): Boolean =
        year < 1900 || (year == 1900 && month == 1 && day < 31) || year > 2100

    /** 公历大小月：除 2 月外固定 31 天的月份。 */
    private val LONG_MONTHS: Set<Int> = setOf(1, 3, 5, 7, 8, 10, 12)

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /**
     * 传出y年m月d日对应的农历. yearCyl3:农历年与1864的相差数 ? monCyl4:从1900年1月31日以来,闰月数
     * dayCyl5:与1900年1月31日相差的天数,再加40 ?
     *
     *
     * @param
     * @return
     */
    fun getLunarDate(year: Int, month: Int, day: Int): LunarDate {
        if (isOutOfSupportedRange(year, month, day)) {
            return LunarDate(year, month, day, "", "", "", "")
        }
        //农历节假日
        val lDateInt = getLunarDateInt(year, month, day)
        //得到当前年对应的农历年份
        val lYear = lDateInt / 10000
        //得到当前日期对应的阴历月份
        val lMonth = (lDateInt % 10000) / 100
        val lDay = (lDateInt - lYear * 10000 - lMonth * 100)
        val lMonthStr = lMonth.toString().padStart(2, '0')
        val lDayStr = lDay.toString().padStart(2, '0')
        val lY = LunarAlmanac.cyclical(year, month, day) + "年"
        var lM = ""
        val testMonth = getSunDate(lYear, lMonth, lDay, false)[Calendar.MONTH] + 1
        if (testMonth != month) {
            lM = "闰"
        }
        lM += LunarTables.CHINESE_NUMBER[(lMonth - 1) % 12] + "月"
        val lD = LunarAlmanac.getChinaDayString(lDay)
        var animalsYear = 0
        val term2 = LunarAlmanac.solarTerm(year, 2) //立春
        animalsYear = if (month > 2 || (month == 2 && day >= term2)) {
            year
        } else {
            year - 1
        }
        //农历节假日；除夕没有固定「月日」键，按腊月最后一天单独推导
        val lunarFestival = LunarTables.LUNAR_FESTIVAL[lMonthStr + lDayStr]
            ?: newYearEveIfAny(lYear, lMonth, lDay)
        val sMonth = month.toString().padStart(2, '0')
        val sDay = day.toString().padStart(2, '0')
        //公历节假日
        val solarFestival = LunarTables.SOLAR_FESTIVAL[sMonth + sDay]
        var solarTerms: String? = null
        val b = LunarAlmanac.getDateOfSolarTerms(year, month)
        if (day == b / 100) {
            solarTerms = LunarTables.THE_24_SOLAR_TERMS[(month - 1) * 2]
        } else if (day == b % 100) {
            solarTerms = LunarTables.THE_24_SOLAR_TERMS[(month - 1) * 2 + 1]
        }
        return LunarDate(
            year,
            month,
            day,
            animalsYear(animalsYear),
            lY,
            lM,
            lD,
            lunarFestival,
            solarFestival,
            solarTerms
        )
    }

    /**
     * 除夕判定：当天为农历年的最后一天，即「次日是正月初一」。
     *
     * 不能用固定的 `"1230"` 键：腊月大月才是三十，小月只有廿九，写死会漏掉一半年份
     * （例如 2022 年除夕是腊月廿九）。腊月是农历年的最后一个月，因此
     * 「当天是腊月最后一天」与「次日是正月初一」等价，用当月天数判定即可，
     * 无需再多跑一次公历→农历换算。
     *
     * 1900-2100 的数据表（[LunarTables.LUNAR_INFO]）中不存在闰腊月（闰月表里 12 从未标闰，
     * 最近一次闰腊月是 1574 年），故这里不必叠加闰月天数。
     */
    private fun newYearEveIfAny(lYear: Int, lMonth: Int, lDay: Int): String? =
        if (lMonth == 12 && lDay == monthDays(lYear, 12)) "除夕" else null

    fun getDaysOfMonth(year: Int, month: Int): Int { //返回公历year年month月的当月总天数
        return when {
            month == 2 -> if (isLeapYear(year)) 29 else 28
            month in LONG_MONTHS -> 31
            else -> 30
        }
    }

    /*输入
     * year 公历年份，大于1900
     * month 公历月份 1--12
     * 输出
     * ArrayList<String> year年month月全月宜忌
     * */
    fun getDateFromBaseDate(days: Int): Int {
        val year: Int
        val month: Int
        val day: Int
        val cal = Calendar.getInstance()
        cal[Calendar.YEAR] = 1900
        cal[Calendar.MONTH] = 0
        cal[Calendar.DAY_OF_MONTH] = 31
        cal.add(Calendar.DATE, days)
        year = cal[Calendar.YEAR]
        month = cal[Calendar.MONTH] + 1
        day = cal[Calendar.DAY_OF_MONTH]

        return 10000 * year + 100 * month + day
    }

}

data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val animalsYear: String,
    val lunarYear: String,
    val lunarMonth: String,
    val lunarDay: String,
    val lunarFestival: String? = null,
    val solarFestival: String? = null,
    val solarTerms: String? = null,
) {

    fun getDay(): String {
        return "${year}${month.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}"
    }

    fun isFestival(): Boolean {
        return solarFestival != null || lunarFestival != null || solarTerms != null
    }

    fun getFestival(): List<String> {
        return listOfNotNull(solarFestival, lunarFestival, solarTerms)
    }

    fun getFirstFestival(): String {
        val list = getFestival()
        return if (list.isNotEmpty()) list.first() else lunarDay
    }

}