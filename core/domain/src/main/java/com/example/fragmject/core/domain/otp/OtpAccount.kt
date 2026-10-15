package com.example.fragmject.core.domain.otp

/**
 * 一个 OTP 账户（领域模型）。
 *
 * [secret] 是 Base32 明文密钥，**只允许存在于内存**：持久化前必须加密（阶段 2），
 * 且不得写入日志、崩溃上报或任何明文存储。
 */
data class OtpAccount(
    val id: Long = 0,
    val issuer: String = "",
    val accountName: String = "",
    val secret: String = "",
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
    val type: OtpType = OtpType.TOTP,
    val counter: Long = 0,
    val sortOrder: Int = 0,
) {
    /** UI 展示用的发行方：为空时退回账户名。 */
    val displayIssuer: String get() = issuer.ifBlank { accountName }
}
