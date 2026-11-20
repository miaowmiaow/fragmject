package com.example.fragmject.app.ui

/**
 * 「这次页面切换是否由刚才那次点击引起」的判定。
 *
 * ============================ 为什么单独抽出来 ============================
 * 这段逻辑只有三行，但**历史上连出过两次 bug**，且两次都是静默失效
 * （鱼只是不穿越了，没有任何报错）：
 *
 * 1. 时间戳默认值是 0：某个构造点忘了传，`atMillis == 0` 直接被判为"旧点击"，
 *    穿越永久不触发；
 * 2. 用布尔量表示"刚发生切换"：`true → true` 不产生变化，`LaunchedEffect(key)`
 *    不重跑，300ms 内连续两次跳转的第二次丢失。
 *
 * 判定本身是纯函数（时钟由调用方传入），所以可以直接单测；别再把
 * `SystemClock.uptimeMillis()` 写在判断里，那样只能靠真机验证。
 */
internal object TeleportGate {

    /**
     * 点击与页面切换之间的最大时间差。
     *
     * 1s 的依据：正常点击到导航回调在几十毫秒内完成；而"返回手势 / 深链 / 代码内跳转"
     * 没有新点击，最后一次点击可能是几十秒前，用它的坐标穿越会像随机瞬移。
     */
    const val FRESHNESS_MS = 1_000L

    /**
     * 是否应该发起穿越。
     *
     * @param nowMillis 当前时间（`SystemClock.uptimeMillis()`）
     * @param tapAtMillis 最后一次点击的时间；[NO_TAP] 表示从未点击过
     * @param freshnessMs 允许的时间差
     */
    fun shouldTeleport(
        nowMillis: Long,
        tapAtMillis: Long,
        freshnessMs: Long = FRESHNESS_MS,
    ): Boolean {
        if (tapAtMillis == NO_TAP) return false
        val age = nowMillis - tapAtMillis
        // age < 0 说明时钟异常（两处取值不同源），按不可信处理：宁可不穿越，
        // 也不要拿一个来路不明的坐标让鱼瞬移
        return age in 0..freshnessMs
    }

    /** 从未点击过的标记值。 */
    const val NO_TAP = 0L
}
