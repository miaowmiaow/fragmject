package com.example.fragmject.core.domain.otp

/**
 * TOTP：基于时间的一次性密码（RFC 6238）。
 *
 * 时间片计数器 = floor(Unix 秒 / period)（T0 = 0），其余计算与 HOTP 一致。
 * [counterOffset] 用于取相邻时间片（如校验或预览），默认 0 即当前时间片。
 *
 * 完全离线、确定性：只需要密钥与时间，不依赖任何后端。
 */
object Totp {

    fun generate(
        secret: ByteArray,
        timeMillis: Long,
        periodSeconds: Int = 30,
        digits: Int = 6,
        algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        counterOffset: Int = 0,
    ): String {
        require(periodSeconds > 0) { "period 必须为正数，实际 $periodSeconds" }
        val counter = timeMillis / 1000L / periodSeconds + counterOffset
        return Hotp.generate(secret, counter, algorithm, digits)
    }

    /** 当前时间片剩余毫秒数，用于 UI 倒计时。 */
    fun remainingMillis(timeMillis: Long, periodSeconds: Int = 30): Long {
        require(periodSeconds > 0) { "period 必须为正数，实际 $periodSeconds" }
        val periodMillis = periodSeconds * 1000L
        return periodMillis - (timeMillis % periodMillis)
    }
}
