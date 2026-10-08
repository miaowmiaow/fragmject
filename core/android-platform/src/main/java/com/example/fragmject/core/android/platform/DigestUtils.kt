package com.example.fragmject.core.android.platform

import java.security.MessageDigest

/**
 * 摘要计算：提供缓存键等场景需要的十六进制哈希。
 *
 * 收敛到本模块，避免各模块为取一个 md5 而引入 okhttp/okio 之类的传递依赖。
 */
object DigestUtils {

    /** 计算 [raw] 的 MD5 十六进制串（小写，32 位）。 */
    fun md5Hex(raw: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }
}
