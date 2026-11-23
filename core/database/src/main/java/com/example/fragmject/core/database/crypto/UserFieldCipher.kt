package com.example.fragmject.core.database.crypto

import com.example.fragmject.core.android.platform.crypto.KeystoreSecretCipher
import java.util.Base64

/**
 * `user` 表 PII 字段（`username` / `nickname` / `icon`）的字段级加密。
 *
 * 加密边界：
 * - 只加密这三个字段，不做全库加密（成本高、收益低，见加固文档 §3.5「字段级按需」）；
 * - 密钥由 Android Keystore 持有（[KeystoreSecretCipher]），数据库文件被拷走也无法离线还原；
 * - `token` / `password` 本就不落库（`UserEntity` 上是 `@Ignore`），不在此范围。
 *
 * 密文格式：`v1:` + Base64(IV||cipherText)。前缀既是版本位，也是**明文/密文的判别依据**：
 * 没有前缀的旧行按历史明文原样返回，避免把 `abcd` 这类本身就是合法 Base64 的用户名
 * 误解码成乱码。
 *
 * 为什么不升数据库版本做迁移：`user` 行只是登录后的展示缓存（凭证不落库），
 * 老行在用户下次登录/资料刷新时会被 [encryptToText] 覆盖成密文；为一次性改写单行
 * 引入 15→16 迁移与 schema 变更，风险大于收益。
 */
internal class UserFieldCipher(
    private val encryptBlock: (String) -> ByteArray = KeystoreSecretCipher::encrypt,
    private val decryptBlock: (ByteArray) -> String = KeystoreSecretCipher::decrypt,
) {

    /** 加密为可入库的文本；`null` 字段保持 `null`（列可空）。 */
    fun encryptToText(raw: String?): String? =
        raw?.let { PREFIX + Base64.getEncoder().encodeToString(encryptBlock(it)) }

    /**
     * 解密；解不开（密钥被清除、数据被篡改）返回 `null`，不抛异常。
     *
     * 没有 [PREFIX] 前缀的值是本次改动之前写入的历史明文，原样返回 —— 此时不做任何
     * 解码尝试，否则 `abcd` 这类合法 Base64 的用户名会被解成乱码。
     */
    fun decryptOrPlainText(stored: String?): String? {
        if (stored == null) return null
        if (!stored.startsWith(PREFIX)) return stored
        return runCatching {
            decryptBlock(Base64.getDecoder().decode(stored.removePrefix(PREFIX)))
        }.getOrNull()
    }

    private companion object {
        const val PREFIX = "v1:"
    }
}
