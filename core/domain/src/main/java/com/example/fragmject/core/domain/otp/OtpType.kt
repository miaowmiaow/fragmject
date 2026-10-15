package com.example.fragmject.core.domain.otp

/** OTP 类型：`totp`（基于时间，RFC 6238）或 `hotp`（基于计数器，RFC 4226）。 */
enum class OtpType(val wireValue: String) {
    HOTP("hotp"),
    TOTP("totp"),
    ;

    companion object {
        /** URI 取值 → 枚举；非 totp/hotp 返回 null（调用方按解析失败处理）。 */
        fun fromWireValue(value: String?): OtpType? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }

        /** 持久化序号 → 枚举；**枚举顺序即持久化契约，不得调整顺序**。 */
        fun fromOrdinal(value: Int): OtpType = entries.getOrElse(value) { TOTP }
    }
}
