package com.example.fragmject.core.database.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * user 表 PII 字段的字段级加密。
 *
 * 用假的加解密函数覆盖「文本包装 / 历史明文兼容 / 解密失败降级」三段逻辑；
 * 真正的 Keystore 加密由 `core:android-platform` 的 `KeystoreSecretCipherTest`（真机）覆盖。
 */
class UserFieldCipherTest {

    private val cipher = UserFieldCipher(
        encryptBlock = { "enc".toByteArray() + it.toByteArray() },
        decryptBlock = { bytes ->
            val text = String(bytes)
            require(text.startsWith("enc")) { "bad cipher" }
            text.removePrefix("enc")
        },
    )

    @Test
    fun `加密结果带版本前缀且不含原文`() {
        val stored = cipher.encryptToText("张小明")
        assertTrue(stored!!.startsWith("v1:"))
        assertFalse(stored.contains("张小明"))
        assertEquals("张小明", cipher.decryptOrPlainText(stored))
    }

    @Test
    fun `null 字段保持 null`() {
        assertNull(cipher.encryptToText(null))
        assertNull(cipher.decryptOrPlainText(null))
    }

    @Test
    fun `无前缀的历史明文原样返回`() {
        // `abcd` 本身是合法 Base64：没有前缀时不能尝试解码，否则会被解成乱码
        assertEquals("abcd", cipher.decryptOrPlainText("abcd"))
        assertEquals("老昵称", cipher.decryptOrPlainText("老昵称"))
    }

    @Test
    fun `带前缀但解不开时返回 null 而不是抛异常`() {
        assertNull(cipher.decryptOrPlainText("v1:Zm9v"))
    }
}
