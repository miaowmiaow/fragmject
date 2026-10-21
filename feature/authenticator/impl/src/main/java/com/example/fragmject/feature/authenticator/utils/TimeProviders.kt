package com.example.fragmject.feature.authenticator.utils

/** 视为「明显漂移」的阈值：低于该值不打扰用户。 */
const val DRIFT_THRESHOLD_MS = 30_000L

/**
 * 当前时间来源。
 *
 * 抽成接口是为了让 `AuthenticatorViewModel` 可单测：TOTP 的结果完全由
 * 「密钥 + 时间」决定，测试必须能把时间钉死，不能依赖 `System.currentTimeMillis()`。
 */
fun interface Clock {
    fun nowMillis(): Long
}

/**
 * 设备时间与标准时间的偏移来源（正数 = 设备慢了）。
 *
 * 生产实现走 SNTP；测试注入固定值，从而覆盖「漂移过大提示」与
 * 「查询失败静默降级」两条分支，不必真的发 UDP 包。
 */
fun interface TimeOffsetProvider {
    suspend fun offsetMillis(): Long?
}

/** 生产时钟：唯一允许直接读系统时间的地方。 */
object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** 生产漂移探测：委托 [SntpTimeSource]（UDP 不通时返回 null）。 */
object SntpOffsetProvider : TimeOffsetProvider {
    override suspend fun offsetMillis(): Long? = SntpTimeSource.queryOffsetMillis()
}
