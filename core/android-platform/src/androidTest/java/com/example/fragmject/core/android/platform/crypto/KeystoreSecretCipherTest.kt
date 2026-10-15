package com.example.fragmject.core.android.platform.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.AEADBadTagException

/**
 * `KeystoreSecretCipher` 的真机测试：**必须在设备上跑**。
 *
 * `AndroidKeyStore` provider 与 SunJCE 行为不同，纯 JVM 单测 [AesGcmCipherTest]
 * 覆盖不到它最要命的一条限制——加密时不允许调用方提供 IV
 * （`InvalidAlgorithmParameterException: Caller-provided IV not permitted`）。
 * 这个差异曾导致「JVM 单测全绿、真机一保存就崩」，故在此加真机回归。
 */
@RunWith(AndroidJUnit4::class)
class KeystoreSecretCipherTest {

    @Test
    fun encryptThenDecryptReturnsOriginal() {
        val plain = "JBSWY3DPEHPK3PXP"

        val packed = KeystoreSecretCipher.encrypt(plain)

        assertEquals(plain, KeystoreSecretCipher.decrypt(packed))
    }

    @Test
    fun encryptUsesFreshIvPerCall() {
        val first = KeystoreSecretCipher.encrypt("same")
        val second = KeystoreSecretCipher.encrypt("same")

        assertFalse(first.contentEquals(second))
        assertFalse(first.copyOfRange(0, 12).contentEquals(second.copyOfRange(0, 12)))
    }

    @Test
    fun decryptRejectsTamperedCipherText() {
        val packed = KeystoreSecretCipher.encrypt("JBSWY3DPEHPK3PXP")
        val tampered = packed.copyOf().also { it[it.lastIndex] = (it[it.lastIndex] + 1).toByte() }

        assertThrows(AEADBadTagException::class.java) {
            KeystoreSecretCipher.decrypt(tampered)
        }
    }

    @Test
    fun encryptedPayloadDoesNotContainPlainSecret() {
        val secret = "JBSWY3DPEHPK3PXP"

        val packed = KeystoreSecretCipher.encrypt(secret)

        assertFalse(String(packed, Charsets.ISO_8859_1).contains(secret))
    }
}
