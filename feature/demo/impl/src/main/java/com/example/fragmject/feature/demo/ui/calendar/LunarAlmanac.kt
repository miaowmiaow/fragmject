package com.example.fragmject.feature.demo.ui.calendar

import java.util.ArrayList
import java.util.Calendar
import java.util.Date

/**
 * 农历的「人文展示」部分：生肖、干支、农历日文案、星座、节气日期、宜忌。
 *
 * 自 LunarUtils 拆出：这部分随文案与运营需求变化，与日期换算的稳定性诉求不同，
 * 分文件后 diff 更聚焦。对外仍通过 LunarUtils 的公开函数访问。
 */
internal object LunarAlmanac {

    internal fun animalsYear(year: Int): String {
        val animals = arrayOf(
            "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
        )
        return animals[(year - 4) % 12] + "年"
    }

    // ====== 传入 月日的offset 传回干支, 0=甲子
    internal fun cyclical(year: Int, month: Int, day: Int): String {
        var num = year - 1900 + 36
        //立春日期
        val term2 = solarTerm(year, 2)
        num = if (month > 2 || (month == 2 && day >= term2)) {
            num + 0
        } else {
            num - 1
        }
        return LunarTables.GAN[num % 10] + LunarTables.ZHI[num % 12]
    }

    internal fun getChinaDayString(day: Int): String { //将农历day日格式化成农历表示的字符串
        val chineseTen = arrayOf("初", "十", "廿", "卅")
        val chineseDay = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
        // 整十的日期有固定写法，不能按 十位前缀 + "十" 拼接：
        // 直接拼接会把 30 渲染成「卅十」，而腊月三十是每年的常见日期。
        return when (day) {
            20 -> "二十"
            30 -> "三十"
            else -> chineseTen[(day - 1) / 10] + chineseDay[(day - 1) % 10]
        }
    }

    /*
     * 计算公历nY年nM月nD日和bY年bM月bD日渐相差多少天
     * */
    internal fun getConstellation(month: Int, day: Int): String { //计算星座
        val date = month * 100 + day
        return when (date) {
            in 120..218 -> {
                LunarTables.CONSTELLATIONS[1]
            }

            in 219..320 -> {
                LunarTables.CONSTELLATIONS[2]
            }

            in 321..419 -> {
                LunarTables.CONSTELLATIONS[3]
            }

            in 420..520 -> {
                LunarTables.CONSTELLATIONS[4]
            }

            in 521..620 -> {
                LunarTables.CONSTELLATIONS[5]
            }

            in 621..721 -> {
                LunarTables.CONSTELLATIONS[6]
            }

            in 722..822 -> {
                LunarTables.CONSTELLATIONS[7]
            }

            in 823..922 -> {
                LunarTables.CONSTELLATIONS[8]
            }

            in 923..1022 -> {
                LunarTables.CONSTELLATIONS[9]
            }

            in 1023..1121 -> {
                LunarTables.CONSTELLATIONS[10]
            }

            in 1122..1221 -> {
                LunarTables.CONSTELLATIONS[11]
            }

            else -> {
                LunarTables.CONSTELLATIONS[0]
            }
        }
    }

    internal fun solarTerm(y: Int, n: Int): Int {
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

    internal fun getDateOfSolarTerms(year: Int, month: Int): Int {
        val a = solarTerm(year, (month - 1) * 2)
        val b = solarTerm(year, (month - 1) * 2 + 1)
        return a * 100 + b
        //return 0;
    }

    internal fun jcrt(d: String): String {
        var jcrjxt = ""
        val yj0 = "宜:\t"
        val yj1 = "忌:\t"
        val br = "-"
        //String yj0 = "",yj1 = "",br = "-";
        if (d == "建") jcrjxt = yj0 + LunarTables.YI[0] + br + yj1 + LunarTables.JI[0]
        if (d == "除") jcrjxt = yj0 + LunarTables.YI[1] + br + yj1 + LunarTables.JI[1]
        if (d == "满") jcrjxt = yj0 + LunarTables.YI[2] + br + yj1 + LunarTables.JI[2]
        if (d == "平") jcrjxt = yj0 + LunarTables.YI[3] + br + yj1 + LunarTables.JI[3]
        if (d == "定") jcrjxt = yj0 + LunarTables.YI[4] + br + yj1 + LunarTables.JI[4]
        if (d == "执") jcrjxt = yj0 + LunarTables.YI[5] + br + yj1 + LunarTables.JI[5]
        if (d == "破") jcrjxt = yj0 + LunarTables.YI[6] + br + yj1 + LunarTables.JI[6]
        if (d == "危") jcrjxt = yj0 + LunarTables.YI[7] + br + yj1 + LunarTables.JI[7]
        if (d == "成") jcrjxt = yj0 + LunarTables.YI[8] + br + yj1 + LunarTables.JI[8]
        if (d == "收") jcrjxt = yj0 + LunarTables.YI[9] + br + yj1 + LunarTables.JI[9]
        if (d == "开") jcrjxt = yj0 + LunarTables.YI[10] + br + yj1 + LunarTables.JI[10]
        if (d == "闭") jcrjxt = yj0 + LunarTables.YI[11] + br + yj1 + LunarTables.JI[11]

        return jcrjxt
    }

    /* num_y%12, num_m%12, num_d%12, num_y%10, num_d%10
     * m:农历月份 1---12
     * dt：农历日
     * */
    /**
     * 十二地支相冲（六冲）的 (支1, 支2) 组合；双向成立，故两向都列出。
     */
    private val SUI_PO_PAIRS: Set<Pair<Int, Int>> = setOf(
        0 to 6, 6 to 0, 1 to 7, 7 to 1, 2 to 8, 8 to 2,
        3 to 9, 9 to 3, 4 to 10, 10 to 4, 5 to 11, 11 to 5,
    )

    /** 日值上朔：(年支, 日干支数字串)。 */
    private val SHANG_SUO_DAYS: Set<Pair<Int, String>> = setOf(
        0 to "911", 1 to "55", 2 to "111", 3 to "75", 4 to "311",
        5 to "95", 6 to "511", 7 to "15", 8 to "711", 9 to "35",
    )

    /** 杨公十三忌：(农历月, 农历日)。 */
    private val YANG_GONG_FORBIDDEN_DAYS: Set<Pair<Int, Int>> = setOf(
        1 to 13, 2 to 11, 3 to 9, 4 to 7, 5 to 5, 6 to 3,
        7 to 1, 7 to 29, 8 to 27, 9 to 25, 10 to 23, 11 to 21, 12 to 19,
    )

    internal fun calConv2(yy: Int, mm: Int, dd: Int, y: Int, d: Int, m: Int, dt: Int): String? {
        val dy = d.toString() + "" + dd

        return if ((yy to dd) in SUI_PO_PAIRS) {
            /*
                  * 地支共有六对相冲，俗称“六冲”，即：子午相冲、丑未相冲、寅申相冲、卯酉相冲、辰戌相冲、巳亥相冲。
                  * 如当年是甲子年，子为太岁，与子相冲者是午，假如当日是午日，则为岁破，其余的以此类推，即丑年的岁破为未日，
                  * 寅年的岁破为申日，卯年的岁破为酉日，辰年的岁破为戌日，巳年的岁破为亥日，午年的岁破为子日，未年的岁破为丑日，
                  * 申年的岁破为寅日，酉年的岁破为卯日，戌年的岁破为辰日，亥年的岁破为巳日。
                  * */
            "日值岁破 大事不宜"
        } else if ((mm to dd) in SUI_PO_PAIRS) {
            "日值月破 大事不宜"
        } else if ((y to dy) in SHANG_SUO_DAYS) {
            "日值上朔 大事不宜"
        } else if ((m to dt) in YANG_GONG_FORBIDDEN_DAYS) {
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

    internal fun getYiJi(year: Int, month: Int): ArrayList<String?> {
        var num_y = -1
        var num_m = (year - 1900) * 12 + month - 1 + 12
        var num_d: Int
        var mLMonth = 1
        var mLDay = 1
        var mLun_x = 0
        val days_of_month = LunarUtils.getDaysOfMonth(year, month)
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
                val `var` = LunarUtils.getLunarDateInt(year, month, i + 1)
                val mLYear = `var` / 10000
                mLMonth = (`var` % 10000) / 100
                mLDay = (`var` - mLYear * 10000 - mLMonth * 100)
                mLun_x =
                    if ((LunarUtils.leapMonth(mLYear) != 0)) LunarUtils.leapDays(mLYear) else LunarUtils.monthDays(mLYear, mLMonth)
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
            num_d = (LunarUtils.getDaysOfTwoDate(1900, 1, 1, year, month, 1) + 10) + i

            mLDay++
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
                val `var` = LunarTables.JC[num_m % 12][num_d % 12]
                str = jcrt(`var`)
            }
            yiji.add(str)
        }

        return yiji
    }

    /*
     * 公历某日宜忌
     * */
    internal fun getYiJi(year: Int, month: Int, day: Int): String {
        var day = day
        var num_y = -1
        var num_m = (year - 1900) * 12 + month - 1 + 12
        val num_d: Int
        var mLMonth = 1
        var mLDay = 1
        val days_of_month = LunarUtils.getDaysOfMonth(year, month)

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
        num_d = (LunarUtils.getDaysOfTwoDate(1900, 1, 1, year, month, 1) + 10) + day - 1

        val `var` = LunarUtils.getLunarDateInt(year, month, day)
        val mLYear = `var` / 10000
        mLMonth = (`var` % 10000) / 100
        mLDay = (`var` - mLYear * 10000 - mLMonth * 100)


        var str =
            calConv2(num_y % 12, num_m % 12, num_d % 12, num_y % 10, num_d % 10, mLMonth, mLDay)
        if (str == null) {
            str = jcrt(LunarTables.JC[num_m % 12][num_d % 12])
        }
        return str
    }

    /*
     * 计算距离1900年12月31日days天后的日期
     * */
}
