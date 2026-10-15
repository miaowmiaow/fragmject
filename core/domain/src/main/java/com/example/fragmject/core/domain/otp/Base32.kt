package com.example.fragmject.core.domain.otp

/**
 * Base32（RFC 4648）编解码，用于认证器密钥。
 *
 * 密钥通常以 Base32 字符串分发（二维码或手动输入）。解码容忍大小写、空格、
 * 连字符与尾部 `=` 填充；但**任何非法字符一律返回 null，绝不静默忽略**——
 * 静默忽略会得到一个错误的密钥，表现为「验证码一直不对」这种极难排查的问题。
 *
 * 编码不输出 `=` 填充，与主流验证器的展示形式一致。
 */
object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(bytes: ByteArray): String {
        val bits = StringBuilder(bytes.size * 8)
        for (b in bytes) {
            bits.append((b.toInt() and 0xFF).toString(radix = 2).padStart(8, '0'))
        }
        val out = StringBuilder()
        var index = 0
        while (index < bits.length) {
            val chunk = bits.substring(index, minOf(index + 5, bits.length)).padEnd(5, '0')
            out.append(ALPHABET[chunk.toInt(radix = 2)])
            index += 5
        }
        return out.toString()
    }

    /** 解码失败（空串或含非法字符）返回 null。 */
    fun decode(value: String): ByteArray? {
        val normalized = value
            .replace(" ", "")
            .replace("-", "")
            .uppercase()
            .trimEnd('=')
        if (normalized.isEmpty()) return null
        val bits = StringBuilder(normalized.length * 5)
        for (c in normalized) {
            val index = ALPHABET.indexOf(c)
            if (index < 0) return null
            bits.append(index.toString(radix = 2).padStart(5, '0'))
        }
        // 尾部不足 8 位的剩余比特是编码产生的填充位，直接丢弃
        val out = ByteArray(bits.length / 8)
        for (i in out.indices) {
            out[i] = bits.substring(i * 8, i * 8 + 8).toInt(radix = 2).toByte()
        }
        return out
    }

    fun isValid(value: String): Boolean = decode(value) != null
}
