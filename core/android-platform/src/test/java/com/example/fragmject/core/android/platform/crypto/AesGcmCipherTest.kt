package com.example.fragmject.core.android.platform.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM 加解密验证。
 *
 * 本类只依赖 JCA，用 [SecretKeySpec] 构造测试密钥即可在纯 JVM 下运行——不需要
 * Keystore、Robolectric 或真机。这正是把「算法」与「密钥来源」拆开的目的。
 */
class AesGcmCipherTest {

    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private val otherKey = SecretKeySpec(ByteArray(32) { (it + 7).toByte() }, "AES")

    @Test
    fun `round trips plain bytes`() {
        val plain = "JBSWY3DPEHPK3PXP".toByteArray()
        assertArrayEquals(plain, AesGcmCipher.decrypt(key, AesGcmCipher.encrypt(key, plain)))
    }

    @Test
    fun `same plain text yields different cipher text each time`() {
        val plain = "secret".toByteArray()
        val first = AesGcmCipher.encrypt(key, plain)
        val second = AesGcmCipher.encrypt(key, plain)
        assertFalse("随机 IV 应使两次密文不同", first.contentEquals(second))
        assertArrayEquals(plain, AesGcmCipher.decrypt(key, second))
    }

    @Test
    fun `iv prefix differs between encryptions`() {
        val plain = "x".toByteArray()
        val first = AesGcmCipher.encrypt(key, plain)
        val second = AesGcmCipher.encrypt(key, plain)
        assertFalse(first.copyOfRange(0, 12).contentEquals(second.copyOfRange(0, 12)))
    }

    @Test
    fun `tampered cipher text is rejected instead of yielding garbage`() {
        val packed = AesGcmCipher.encrypt(key, "secret".toByteArray())
        val tampered = packed.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertThrows(Exception::class.java) { AesGcmCipher.decrypt(key, tampered) }
    }

    @Test
    fun `wrong key cannot decrypt`() {
        val packed = AesGcmCipher.encrypt(key, "secret".toByteArray())
        assertThrows(Exception::class.java) { AesGcmCipher.decrypt(otherKey, packed) }
    }

    @Test
    fun `cipher text does not contain the plain secret`() {
        val secret = "JBSWY3DPEHPK3PXP"
        val packed = AesGcmCipher.encrypt(key, secret.toByteArray())
        assertFalse(
            "密文中不应出现明文密钥",
            String(packed, Charsets.ISO_8859_1).contains(secret),
        )
    }

    @Test
    fun `empty plain text round trips`() {
        val packed = AesGcmCipher.encrypt(key, ByteArray(0))
        assertArrayEquals(ByteArray(0), AesGcmCipher.decrypt(key, packed))
    }
}
