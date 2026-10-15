package com.example.fragmject.core.domain.otp

/**
 * OTP 使用的 HMAC 算法（RFC 6238 / RFC 4226）。
 *
 * - [macName]：传给 `javax.crypto.Mac` 的算法名
 * - [wireValue]：`otpauth://` URI 中 `algorithm` 参数的取值
 */
enum class OtpAlgorithm(
    val macName: String,
    val wireValue: String,
) {
    SHA1("HmacSHA1", "SHA1"),
    SHA256("HmacSHA256", "SHA256"),
    SHA512("HmacSHA512", "SHA512"),
    ;

    companion object {
        /** URI 取值 → 枚举；未知或缺省按 RFC 默认取 [SHA1]。 */
        fun fromWireValue(value: String?): OtpAlgorithm =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: SHA1

        /**
         * 持久化序号 → 枚举。**枚举顺序即持久化契约，不得调整顺序**；
         * 越界时退回 [SHA1]，避免老数据读不出来直接崩溃。
         */
        fun fromOrdinal(value: Int): OtpAlgorithm = entries.getOrElse(value) { SHA1 }
    }
}
