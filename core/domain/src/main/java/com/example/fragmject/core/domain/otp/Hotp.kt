package com.example.fragmject.core.domain.otp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HOTP：基于计数器的一次性密码（RFC 4226），是 TOTP 的计算基础。
 *
 * 流程：HMAC(算法, 密钥, 计数器大端 8 字节) → 动态截断 → 取低位 [digits] 位。
 */
object Hotp {

    fun generate(
        secret: ByteArray,
        counter: Long,
        algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        digits: Int = 6,
    ): String {
        require(digits in 1..10) { "digits 必须在 1..10，实际 $digits" }
        val hash = Mac.getInstance(algorithm.macName).run {
            init(SecretKeySpec(secret, algorithm.macName))
            doFinal(counter.toBigEndianBytes())
        }
        val code = dynamicTruncate(hash) % pow10(digits)
        return code.toString().padStart(digits, '0')
    }

    /**
     * RFC 4226 §5.4 动态截断：以哈希最后一个字节的低 4 位为偏移，
     * 取随后 4 个字节并屏蔽最高符号位，得到一个 31 位非负整数。
     */
    private fun dynamicTruncate(hash: ByteArray): Long {
        val offset = hash[hash.size - 1].toInt() and 0x0F
        return (
            ((hash[offset].toInt() and 0x7F).toLong() shl 24) or
                ((hash[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((hash[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (hash[offset + 3].toInt() and 0xFF).toLong()
            ) and 0xFFFFFFFFL
    }

    private fun Long.toBigEndianBytes(): ByteArray = ByteArray(8).also { out ->
        for (i in 0..7) out[i] = (this ushr (8 * (7 - i))).toByte()
    }

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }
}
