package com.example.fragmject.feature.demo.ui.calendar

import android.util.Log
import java.text.ParseException
import java.util.Calendar
import java.util.Date

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

    // ====== 传回农历 y年的生肖
    fun getDaysOfTwoDate(bY: Int, bM: Int, bD: Int, nY: Int, nM: Int, nD: Int): Int {
        var baseDate: Date? = null
        var nowaday: Date? = null
        try {
            baseDate = LunarTables.CHINESE_DATE_FORMAT.parse(bY.toString() + "年" + bM + "月" + bD + "日")
            nowaday = LunarTables.CHINESE_DATE_FORMAT.parse(nY.toString() + "年" + nM + "月" + nD + "日")
        } catch (e: ParseException) {
            Log.e("LunarUtils", "getDaysOfTwoDate parse failed", e)
        }
        // 求出相差的天数；任一日期解析失败则返回 0，避免 NPE
        val base = baseDate ?: return 0
        val now = nowaday ?: return 0
        return ((now.time - base.time) / 86400000L).toInt()
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
        val lYear: Int
        val lMonth: Int
        val lDay: Int
        var daysOfYear = 0
        // 求出和1900年1月31日相差的天数
        //year =1908;
        //month = 3;
        //day =3;
        var offset = getDaysOfTwoDate(1900, 1, 31, year, month, day)
        // 用offset减去每农历年的天数
        // 计算当天是农历第几天
        // i最终结果是农历的年份
        // offset是当年的第几天
        var iYear = 1900
        while (iYear < 2100 && offset > 0) {
            daysOfYear = yearDays(iYear)
            offset -= daysOfYear
            iYear++
        }

        if (offset < 0) {
            offset += daysOfYear
            iYear--
        }
        // 农历年份
        lYear = iYear

        val leapMonth = leapMonth(iYear) // 闰哪个月,1-12
        var leap = false


        // 用当年的天数offset,逐个减去每月（农历）的天数，求出当天是本月的第几天
        var iMonth = 1
        var daysOfMonth = 0
        iMonth = 1
        while (iMonth < 13 && offset > 0) {
            // 闰月
            if (leapMonth > 0 && iMonth == (leapMonth + 1) && !leap) {
                --iMonth
                leap = true
                daysOfMonth = leapDays(iYear)
            } else {
                daysOfMonth = monthDays(iYear, iMonth)
            }
            // 解除闰月
            if (leap && iMonth == (leapMonth + 1)) leap = false

            offset -= daysOfMonth
            iMonth++
        }
        // offset为0时，并且刚才计算的月份是闰月，要校正
        if (offset == 0 && leapMonth > 0 && iMonth == leapMonth + 1) {
            if (leap) {
                leap = false
            } else {
                leap = true
                --iMonth
            }
        }
        // offset小于0时，也要校正
        if (offset < 0) {
            offset += daysOfMonth
            --iMonth
        }
        lMonth = iMonth
        lDay = offset + 1
        return lYear * 10000 + lMonth * 100 + lDay
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

    /**
     * 传出y年m月d日对应的农历. yearCyl3:农历年与1864的相差数 ? monCyl4:从1900年1月31日以来,闰月数
     * dayCyl5:与1900年1月31日相差的天数,再加40 ?
     *
     *
     * @param
     * @return
     */
    fun getLunarDate(year: Int, month: Int, day: Int): LunarDate {
        if (year < 1900 || (year == 1900 && month == 1 && day < 31) || year > 2100) {
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
        //农历节假日
        val lunarFestival = LunarTables.LUNAR_FESTIVAL[lMonthStr + lDayStr]
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

    fun getDaysOfMonth(year: Int, month: Int): Int { //返回公历year年month月的当月总天数
        if (month == 2) {
            return (if (((year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0))) 29 else 28)
        } else if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            return 31
        }
        return 30
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