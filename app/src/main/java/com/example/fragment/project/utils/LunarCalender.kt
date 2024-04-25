package com.example.fragment.project.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getLunarDate(year: Int, month: Int, day: Int): DateInfo {
    return LunarCalender.getInstance().getLunarDate(year, month, day)
}

fun getDaysOfTwoDate(bY: Int, bM: Int, bD: Int, nY: Int, nM: Int, nD: Int): Int {
    return LunarCalender.getInstance().getDaysOfTwoDate(bY, bM, bD, nY, nM, nD)
}

/**
 * 中国农历工具类-
 */
class LunarCalender private constructor() {

    companion object {

        @Volatile
        private var INSTANCE: LunarCalender? = null

        fun getInstance() = INSTANCE ?: synchronized(LunarCalender::class.java) {
            INSTANCE ?: LunarCalender().also { INSTANCE = it }
        }

        val CHINESE_DATE_FORMAT = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)

        val CHINESE_NUMBER = arrayOf(
            "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
        )

        val LUNAR_INFO = longArrayOf(
            0x4bd8, 0x4ae0, 0xa570, 0x54d5, 0xd260, 0xd950, 0x5554, 0x56af, 0x9ad0, 0x55d2,
            0x4ae0, 0xa5b6, 0xa4d0, 0xd250, 0xd255, 0xb54f, 0xd6a0, 0xada2, 0x95b0, 0x4977,
            0x497f, 0xa4b0, 0xb4b5, 0x6a50, 0x6d40, 0xab54, 0x2b6f, 0x9570, 0x52f2, 0x4970,
            0x6566, 0xd4a0, 0xea50, 0x6a95, 0x5adf, 0x2b60, 0x86e3, 0x92ef, 0xc8d7, 0xc95f,
            0xd4a0, 0xd8a6, 0xb55f, 0x56a0, 0xa5b4, 0x25df, 0x92d0, 0xd2b2, 0xa950, 0xb557,
            0x6ca0, 0xb550, 0x5355, 0x4daf, 0xa5b0, 0x4573, 0x52bf, 0xa9a8, 0xe950, 0x6aa0,
            0xaea6, 0xab50, 0x4b60, 0xaae4, 0xa570, 0x5260, 0xf263, 0xd950, 0x5b57, 0x56a0,
            0x96d0, 0x4dd5, 0x4ad0, 0xa4d0, 0xd4d4, 0xd250, 0xd558, 0xb540, 0xb6a0, 0x95a6,
            0x95bf, 0x49b0, 0xa974, 0xa4b0, 0xb27a, 0x6a50, 0x6d40, 0xaf46, 0xab60, 0x9570,
            0x4af5, 0x4970, 0x64b0, 0x74a3, 0xea50, 0x6b58, 0x5ac0, 0xab60, 0x96d5, 0x92e0,
            0xc960, 0xd954, 0xd4a0, 0xda50, 0x7552, 0x56a0, 0xabb7, 0x25d0, 0x92d0, 0xcab5,
            0xa950, 0xb4a0, 0xbaa4, 0xad50, 0x55d9, 0x4ba0, 0xa5b0, 0x5176, 0x52bf, 0xa930,
            0x7954, 0x6aa0, 0xad50, 0x5b52, 0x4b60, 0xa6e6, 0xa4e0, 0xd260, 0xea65, 0xd530,
            0x5aa0, 0x76a3, 0x96d0, 0x4afb, 0x4ad0, 0xa4d0, 0xd0b6, 0xd25f, 0xd520, 0xdd45,
            0xb5a0, 0x56d0, 0x55b2, 0x49b0, 0xa577, 0xa4b0, 0xaa50, 0xb255, 0x6d2f, 0xada0,
            0x4b63, 0x937f, 0x49f8, 0x4970, 0x64b0, 0x68a6, 0xea5f, 0x6b20, 0xa6c4, 0xaaef,
            0x92e0, 0xd2e3, 0xc960, 0xd557, 0xd4a0, 0xda50, 0x5d55, 0x56a0, 0xa6d0, 0x55d4,
            0x52d0, 0xa9b8, 0xa950, 0xb4a0, 0xb6a6, 0xad50, 0x55a0, 0xaba4, 0xa5b0, 0x52b0,
            0xb273, 0x6930, 0x7337, 0x6aa0, 0xad50, 0x4b55, 0x4b6f, 0xa570, 0x54e4, 0xd260,
            0xe968, 0xd520, 0xdaa0, 0x6aa6, 0x56df, 0x4ae0, 0xa9d4, 0xa4d0, 0xd150, 0xf252,
            0xd520
        )

        // 农历部分假日
        val LUNAR_FESTIVAL = mapOf(
            "0101" to "春节",
            "0115" to "元宵节",
            "0505" to "端午节",
            "0707" to "七夕节",
            "0715" to "中元节",
            "0815" to "中秋节",
            "0909" to "重阳节",
            "1208" to "腊八节",
            "1222" to "北小年",
            "1223" to "南小年",
            "0100" to "除夕"
        )

        // 公历部分节假日
        val SOLAR_FESTIVAL = mapOf(
            "0101" to "元旦",
            "0110" to "中国110宣传日",
            "0214" to "情人节",
            "0308" to "妇女节",
            "0312" to "植树节",
            "0315" to "消费者权益日",
            "0401" to "愚人节",
            "0501" to "劳动节",
            "0504" to "青年节",
            "0601" to "儿童节",
            "0626" to "国际禁毒日",
            "0701" to "建党节",  //1921
            "0707" to "七七事变",
            "0801" to "建军节",  //1933
            "0910" to "教师节",
            "0918" to "九一八事变",
            "1001" to "国庆节",
            "1101" to "万圣节",
            "1213" to "国家公祭日",
            "1224" to "平安夜",
            "1225" to "圣诞节",
        )

        //24节气
        val THE_24_SOLAR_TERMS = arrayOf(
            // 时节 气候
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
        )

        //星座
        val CONSTELLATIONS = arrayOf(
            "摩蝎座:12.22—01.19", "水瓶座:01.20—02.18", "双鱼座:02.19—03.20", "白羊座:03.21—04.19",
            "金牛座:04.20—05.20", "双子座:05.21—06.20", "巨蟹座:06.21—07.21", "狮子座:07.22—08.22",
            "处女座:08.23—09.22", "天秤座:09.23—10.22", "天蝎座:10.23—11.21", "射手座:11.22—12.21"
        )

        //宜
        val YI = arrayOf(
            "出行.上任.会友.上书.见工", "除服.疗病.出行.拆卸.入宅",
            "祈福.祭祀.结亲.开市.交易", "祭祀.修填.涂泥.余事勿取",
            "交易.立券.会友.签约.纳畜", "祈福.祭祀.求子.结婚.立约",
            "求医.赴考.祭祀.余事勿取", "经营.交易.求官.纳畜.动土",
            "祈福.入学.开市.求医.成服", "祭祀.求财.签约.嫁娶.订盟",
            "疗病.结婚.交易.入仓.求职", "祭祀.交易.收财.安葬"
        )

        //忌
        val JI = arrayOf(
            "动土.开仓.嫁娶.纳采", "求官.上任.开张.搬家.探病",
            "服药.求医.栽种.动土.迁移", "移徙.入宅.嫁娶.开市.安葬",
            "种植.置业.卖田.掘井.造船", "开市.交易.搬家.远行",
            "动土.出行.移徙.开市.修造", "登高.行船.安床.入宅.博彩",
            "词讼.安门.移徙", "开市.安床.安葬.入宅.破土",
            "安葬.动土.针灸", "宴会.安床.出行.嫁娶.移徙"
        )

        //十二建除
        val JC = arrayOf(
            arrayOf("建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭"),
            arrayOf("闭", "建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开"),
            arrayOf("开", "闭", "建", "除", "满", "平", "定", "执", "破", "危", "成", "收"),
            arrayOf("收", "开", "闭", "建", "除", "满", "平", "定", "执", "破", "危", "成"),
            arrayOf("成", "收", "开", "闭", "建", "除", "满", "平", "定", "执", "破", "危"),
            arrayOf("危", "成", "收", "开", "闭", "建", "除", "满", "平", "定", "执", "破"),
            arrayOf("破", "危", "成", "收", "开", "闭", "建", "除", "满", "平", "定", "执"),
            arrayOf("执", "破", "危", "成", "收", "开", "闭", "建", "除", "满", "平", "定"),
            arrayOf("定", "执", "破", "危", "成", "收", "开", "闭", "建", "除", "满", "平"),
            arrayOf("平", "定", "执", "破", "危", "成", "收", "开", "闭", "建", "除", "满"),
            arrayOf("满", "平", "定", "执", "破", "危", "成", "收", "开", "闭", "建", "除"),
            arrayOf("除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭", "建")
        )

        //天干
        val GAN = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")

        //地支
        val ZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

    }

    // ====== 传回农历 y年的总天数 1900--2100
    private fun yearDays(y: Int): Int {
        var i: Int
        var sum = 348
        i = 0x8000
        while (i > 0x8) {
            if ((LUNAR_INFO[y - 1900] and i.toLong()) != 0L) sum += 1
            i = i shr 1
        }
        return (sum + leapDays(y))
    }

    // ====== 传回农历 y年闰月的天数
    private fun leapDays(y: Int): Int {
        return if (leapMonth(y) != 0) {
            if ((LUNAR_INFO[y - 1899] and 0xfL) != 0L) 30
            else 29
        } else 0
    }

    // ====== 传回农历 y年闰哪个月 1-12 , 没闰传回 0
    private fun leapMonth(y: Int): Int {
        val lun = LUNAR_INFO[y - 1900] and 0xfL
        return (if (lun == 0xfL) 0 else lun).toInt()
    }

    // ====== 传回农历 y年m月的总天数
    private fun monthDays(y: Int, m: Int): Int {
        return if ((LUNAR_INFO[y - 1900] and (0x10000 shr m).toLong()) == 0L) 29
        else 30
    }

    // ====== 传回农历 y年的生肖
    fun animalsYear(year: Int): String {
        val animals = arrayOf(
            "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
        )
        return animals[(year - 4) % 12] + "年"
    }

    // ====== 传入 月日的offset 传回干支, 0=甲子
    private fun cyclical(year: Int, month: Int, day: Int): String {
        var num = year - 1900 + 36
        //立春日期
        val term2 = solarTerm(year, 2)
        num = if (month > 2 || (month == 2 && day >= term2)) {
            num + 0
        } else {
            num - 1
        }
        return GAN[num % 10] + ZHI[num % 12]
    }

    private fun getChinaDayString(day: Int): String { //将农历day日格式化成农历表示的字符串
        val chineseTen = arrayOf("初", "十", "廿", "卅")
        val chineseDay = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
        return if (day != 20 && day != 30) {
            chineseTen[((day - 1) / 10)] + chineseDay[((day - 1) % 10)]
        } else if (day != 20) {
            chineseTen[(day / 10)] + "十"
        } else {
            "二十"
        }
    }

    /*
     * 计算公历nY年nM月nD日和bY年bM月bD日渐相差多少天
     * */
    fun getDaysOfTwoDate(bY: Int, bM: Int, bD: Int, nY: Int, nM: Int, nD: Int): Int {
        var baseDate: Date? = null
        var nowaday: Date? = null
        try {
            baseDate = CHINESE_DATE_FORMAT.parse(bY.toString() + "年" + bM + "月" + bD + "日")
            nowaday = CHINESE_DATE_FORMAT.parse(nY.toString() + "年" + nM + "月" + nD + "日")
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        // 求出相差的天数
        val offset = ((nowaday!!.time - baseDate!!.time) / 86400000L).toInt()
        return offset
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
        //Log.i("--ss--","公历:"+year+"-"+month+"-"+day+":"+offset);
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
            //Log.i("--ss--","农历:"+iYear+":"+daysOfYear+"/"+offset);
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
            //Log.i("--ss--","农历:"+iYear+"-"+iMonth+":"+daysOfMonth+"/"+offset);
        }
        lMonth = iMonth
        lDay = offset + 1
        //Log.i("--ss--","农历:"+LYear+"-"+LMonth+"-"+LDay);
        return lYear * 10000 + lMonth * 100 + lDay
    }


    fun getLunarString(year: Int, month: Int, day: Int): String {
        val lDateInt = getLunarDateInt(year, month, day)
        val lYear = lDateInt / 10000
        val lMonth = (lDateInt % 10000) / 100
        val lDay = lDateInt - lYear * 10000 - lMonth * 100
        val lY = cyclical(year, month, day) + "年"
        var lM = ""
        val testMonth = getSunDate(lYear, lMonth, lDay, false)[Calendar.MONTH] + 1
        if (testMonth != month) {
            lM = "闰"
        }
        lM += CHINESE_NUMBER[(lMonth - 1) % 12] + "月"
        //int leap = leapMonth(lYear);
        val lD = getChinaDayString(lDay)
        var animalsYear = 0
        val term2 = solarTerm(year, 2) //立春
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
    fun getLunarDate(year: Int, month: Int, day: Int): DateInfo {
        if (year < 1900 || (year == 1900 && month == 1 && day < 31) || year > 2100) {
            return DateInfo(year, month, day, "", "", "", "")
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
        val lY = cyclical(year, month, day) + "年"
        var lM = ""
        val testMonth = getSunDate(lYear, lMonth, lDay, false)[Calendar.MONTH] + 1
        if (testMonth != month) {
            lM = "闰"
        }
        lM += CHINESE_NUMBER[(lMonth - 1) % 12] + "月"
        val lD = getChinaDayString(lDay)
        var animalsYear = 0
        val term2 = solarTerm(year, 2) //立春
        animalsYear = if (month > 2 || (month == 2 && day >= term2)) {
            year
        } else {
            year - 1
        }
        //农历节假日
        val lunarFestival = LUNAR_FESTIVAL[lMonthStr + lDayStr]
        val sMonth = month.toString().padStart(2, '0')
        val sDay = day.toString().padStart(2, '0')
        //公历节假日
        val solarFestival = SOLAR_FESTIVAL[sMonth + sDay]
        var solarTerms: String? = null
        val b = getDateOfSolarTerms(year, month)
        if (day == b / 100) {
            solarTerms = THE_24_SOLAR_TERMS[(month - 1) * 2]
        } else if (day == b % 100) {
            solarTerms = THE_24_SOLAR_TERMS[(month - 1) * 2 + 1]
        }
        return DateInfo(
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

    fun getConstellation(month: Int, day: Int): String { //计算星座
        val date = month * 100 + day
        return when (date) {
            in 120..218 -> {
                CONSTELLATIONS[1]
            }

            in 219..320 -> {
                CONSTELLATIONS[2]
            }

            in 321..419 -> {
                CONSTELLATIONS[3]
            }

            in 420..520 -> {
                CONSTELLATIONS[4]
            }

            in 521..620 -> {
                CONSTELLATIONS[5]
            }

            in 621..721 -> {
                CONSTELLATIONS[6]
            }

            in 722..822 -> {
                CONSTELLATIONS[7]
            }

            in 823..922 -> {
                CONSTELLATIONS[8]
            }

            in 923..1022 -> {
                CONSTELLATIONS[9]
            }

            in 1023..1121 -> {
                CONSTELLATIONS[10]
            }

            in 1122..1221 -> {
                CONSTELLATIONS[11]
            }

            else -> {
                CONSTELLATIONS[0]
            }
        }
    }

    private fun solarTerm(y: Int, n: Int): Int {
        val solarTermInfo = intArrayOf(
            0, 21208, 42467, 63836, 85337, 107014,
            128867, 150921, 173149, 195551, 218072, 240693,
            263343, 285989, 308563, 331033, 353350, 375494,
            397447, 419210, 440795, 462224, 483532, 504758
        )
        val cal = Calendar.getInstance()
        cal[1900, 0, 6, 2, 5] = 0
        val temp = cal.time.time
        cal.time = Date(((31556925974.7 * (y - 1900) + solarTermInfo[n] * 60000L) + temp).toLong())
        val a = cal[Calendar.DAY_OF_MONTH]
        return a
    }

    fun getDateOfSolarTerms(year: Int, month: Int): Int {
        val a = solarTerm(year, (month - 1) * 2)
        val b = solarTerm(year, (month - 1) * 2 + 1)
        return a * 100 + b
        //return 0;
    }

    fun jcrt(d: String): String {
        var jcrjxt = ""
        val yj0 = "宜:\t"
        val yj1 = "忌:\t"
        val br = "-"
        //String yj0 = "",yj1 = "",br = "-";
        if (d === "建") jcrjxt = yj0 + YI[0] + br + yj1 + JI[0]
        if (d === "除") jcrjxt = yj0 + YI[1] + br + yj1 + JI[1]
        if (d === "满") jcrjxt = yj0 + YI[2] + br + yj1 + JI[2]
        if (d === "平") jcrjxt = yj0 + YI[3] + br + yj1 + JI[3]
        if (d === "定") jcrjxt = yj0 + YI[4] + br + yj1 + JI[4]
        if (d === "执") jcrjxt = yj0 + YI[5] + br + yj1 + JI[5]
        if (d === "破") jcrjxt = yj0 + YI[6] + br + yj1 + JI[6]
        if (d === "危") jcrjxt = yj0 + YI[7] + br + yj1 + JI[7]
        if (d === "成") jcrjxt = yj0 + YI[8] + br + yj1 + JI[8]
        if (d === "收") jcrjxt = yj0 + YI[9] + br + yj1 + JI[9]
        if (d === "开") jcrjxt = yj0 + YI[10] + br + yj1 + JI[10]
        if (d === "闭") jcrjxt = yj0 + YI[11] + br + yj1 + JI[11]

        return jcrjxt
    }

    /* num_y%12, num_m%12, num_d%12, num_y%10, num_d%10
     * m:农历月份 1---12
     * dt：农历日
     * */
    fun calConv2(yy: Int, mm: Int, dd: Int, y: Int, d: Int, m: Int, dt: Int): String? {
        val dy = d.toString() + "" + dd

        return if ((yy == 0 && dd == 6) || (yy == 6 && dd == 0) || (yy == 1 && dd == 7) ||
            (yy == 7 && dd == 1) || (yy == 2 && dd == 8) || (yy == 8 && dd == 2) ||
            (yy == 3 && dd == 9) || (yy == 9 && dd == 3) || (yy == 4 && dd == 10) ||
            (yy == 10 && dd == 4) || (yy == 5 && dd == 11) || (yy == 11 && dd == 5)
        ) {
            /*
                  * 地支共有六对相冲，俗称“六冲”，即：子午相冲、丑未相冲、寅申相冲、卯酉相冲、辰戌相冲、巳亥相冲。
                  * 如当年是甲子年，子为太岁，与子相冲者是午，假如当日是午日，则为岁破，其余的以此类推，即丑年的岁破为未日，
                  * 寅年的岁破为申日，卯年的岁破为酉日，辰年的岁破为戌日，巳年的岁破为亥日，午年的岁破为子日，未年的岁破为丑日，
                  * 申年的岁破为寅日，酉年的岁破为卯日，戌年的岁破为辰日，亥年的岁破为巳日。
                  * */
            "日值岁破 大事不宜"
        } else if ((mm == 0 && dd == 6) || (mm == 6 && dd == 0) || (mm == 1 && dd == 7) || (mm == 7 && dd == 1) ||
            (mm == 2 && dd == 8) || (mm == 8 && dd == 2) || (mm == 3 && dd == 9) || (mm == 9 && dd == 3) ||
            (mm == 4 && dd == 10) || (mm == 10 && dd == 4) || (mm == 5 && dd == 11) || (mm == 11 && dd == 5)
        ) {
            "日值月破 大事不宜"
        } else if ((y == 0 && dy === "911") || (y == 1 && dy === "55") || (y == 2 && dy === "111") ||
            (y == 3 && dy === "75") || (y == 4 && dy === "311") || (y == 5 && dy === "95") ||
            (y == 6 && dy === "511") || (y == 7 && dy === "15") || (y == 8 && dy === "711") || (y == 9 && dy === "35")
        ) {
            "日值上朔 大事不宜"
        } else if ((m == 1 && dt == 13) || (m == 2 && dt == 11) || (m == 3 && dt == 9) || (m == 4 && dt == 7) ||
            (m == 5 && dt == 5) || (m == 6 && dt == 3) || (m == 7 && dt == 1) || (m == 7 && dt == 29) ||
            (m == 8 && dt == 27) || (m == 9 && dt == 25) || (m == 10 && dt == 23) || (m == 11 && dt == 21) ||
            (m == 12 && dt == 19)
        ) {
            /*
                  * 杨公十三忌   以农历正月十三日始，以后每月提前两天为百事禁忌日
                  * */
            "日值杨公十三忌 大事不宜"
            //}else if(var == getSiLi(m,dt)){
            //return "日值四离  大事勿用";
        } else {
            null
        }
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
    fun getYiJi(year: Int, month: Int): ArrayList<String?> {
        var num_y = -1
        var num_m = (year - 1900) * 12 + month - 1 + 12
        var num_d: Int
        var mLMonth = 1
        var mLDay = 1
        var mLun_x = 0
        val days_of_month = getDaysOfMonth(year, month)
        val yiji = ArrayList<String?>()


        //年柱 1900年立春后为庚子年(60进制36)
        //cyclical(year,month);
        //立春日期
        val term2 = solarTerm(year, 2)


        //月柱  1900年1月小寒以前为 丙子月(60进制12)
        val firstNode = solarTerm(year, (month - 1) * 2) //当月的24节气中的节开始日
        //cyclicalm(num_m);
        //Calendar cal = Calendar.getInstance();
        //cal.set(year, month, 1, 0, 0, 0);
        //1900/1/1与 1970/1/1 相差25567日, 1900/1/1 日柱为甲戌日(60进制10)
        for (i in 0 until days_of_month) {
            if (mLDay > mLun_x) {
                //Log.i("","mLDay > mLun_x "+mLDay+":"+mLun_x);
                val `var` = getLunarDateInt(year, month, i + 1)
                val mLYear = `var` / 10000
                mLMonth = (`var` % 10000) / 100
                mLDay = (`var` - mLYear * 10000 - mLMonth * 100)
                mLun_x =
                    if ((leapMonth(mLYear) != 0)) leapDays(mLYear) else monthDays(mLYear, mLMonth)
                //Log.i("","mLDay > mLun_x ?"+mLDay+":"+mLun_x);
            }
            //依节气调整二月分的年柱, 以立春为界
            if (month == 2 && (i + 1) == term2) {
                //cY = cyclicalm(year-1900 + 36);
                num_y = year - 1900 + 36
            }
            //依节气 调整月柱, 以「节」为界
            if ((i + 1) == firstNode) {
                num_m = (year - 1900) * 12 + month + 12
                //cM = cyclicalm(num_m);
            }
            //日柱
            //cD = cyclicalm(dayCyclical + i);
            num_d = (getDaysOfTwoDate(1900, 1, 1, year, month, 1) + 10) + i

            mLDay++
            //Log.i("","---num_y:"+num_y+","+num_m+","+num_d+",n_m:"+mLMonth+",n_d:"+mLDay+",mLun_x:"+mLun_x);
            var str = calConv2(
                num_y % 12,
                num_m % 12,
                num_d % 12,
                num_y % 10,
                num_d % 10,
                mLMonth,
                mLDay - 1
            )
            if (str == null) {
                val `var` = JC[num_m % 12][num_d % 12]
                str = jcrt(`var`)
                //Log.i("","---"+month+"-"+(i+1)+","+var+":"+str);
            }
            //Log.i("","---"+year+"-"+month+"-"+(i+1)+","+str);
            yiji.add(str)
        }

        return yiji
    }

    /*
     * 公历某日宜忌
     * */
    fun getYiJi(year: Int, month: Int, day: Int): String {
        var day = day
        var num_y = -1
        var num_m = (year - 1900) * 12 + month - 1 + 12
        val num_d: Int
        var mLMonth = 1
        var mLDay = 1
        val days_of_month = getDaysOfMonth(year, month)

        if (day > days_of_month) day = days_of_month


        //年柱 1900年立春后为庚子年(60进制36)
        //cyclical(year,month);
        //立春日期
        val term2 = solarTerm(year, 2)
        val firstNode = solarTerm(year, (month - 1) * 2) //当月的24节气中的节开始日

        if (month == 2 && day == term2) {
            //cY = cyclicalm(year-1900 + 36);//依节气调整二月分的年柱, 以立春为界
            num_y = year - 1900 + 36
        }
        if (day == firstNode) {
            num_m = (year - 1900) * 12 + month + 12 //依节气 调整月柱, 以「节」为界
            //cM = cyclicalm(num_m);
        }
        num_d = (getDaysOfTwoDate(1900, 1, 1, year, month, 1) + 10) + day - 1

        val `var` = getLunarDateInt(year, month, day)
        val mLYear = `var` / 10000
        mLMonth = (`var` % 10000) / 100
        mLDay = (`var` - mLYear * 10000 - mLMonth * 100)


        //Log.i("","---num_y:"+num_y+","+num_m+","+num_d+",n_m:"+mLMonth+",n_d:"+mLDay+",mLun_x:"+mLun_x);
        var str =
            calConv2(num_y % 12, num_m % 12, num_d % 12, num_y % 10, num_d % 10, mLMonth, mLDay)
        if (str == null) {
            str = jcrt(JC[num_m % 12][num_d % 12])
            //Log.i("","---"+month+"-"+(i+1)+","+var+":"+str);
        }
        //Log.i("","---"+year+"-"+month+"-"+(i+1)+","+str);
        return str
    }

    /*
     * 计算距离1900年12月31日days天后的日期
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

data class DateInfo(
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