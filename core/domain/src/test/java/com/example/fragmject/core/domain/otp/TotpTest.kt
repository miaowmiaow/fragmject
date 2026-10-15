package com.example.fragmject.core.domain.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * TOTP 正确性验证：使用 **RFC 6238 附录 B 的官方测试向量**。
 *
 * 向量密钥是 ASCII 字符串 `12345678901234567890`（20 字节），
 * 算法 SHA1、period=30、T0=0、8 位输出。
 */
class TotpTest {

    private val secret = "12345678901234567890".toByteArray(Charsets.US_ASCII)

    @Test
    fun `matches RFC 6238 test vectors with 8 digits`() {
        assertEquals("94287082", totpAt(59, digits = 8))
        assertEquals("07081804", totpAt(1111111109, digits = 8))
        assertEquals("14050471", totpAt(1111111111, digits = 8))
        assertEquals("89005924", totpAt(1234567890, digits = 8))
        assertEquals("69279037", totpAt(2000000000, digits = 8))
        assertEquals("65353130", totpAt(20000000000, digits = 8))
    }

    @Test
    fun `six digit code takes the low 6 digits`() {
        assertEquals("287082", totpAt(59))
        assertEquals("081804", totpAt(1111111109))
        assertEquals("050471", totpAt(1111111111))
        assertEquals("005924", totpAt(1234567890))
        assertEquals("279037", totpAt(2000000000))
        assertEquals("353130", totpAt(20000000000))
    }

    @Test
    fun `code is stable within a time step and changes across steps`() {
        val firstStep = totpAt(30)
        assertEquals(firstStep, totpAt(31))
        assertEquals(firstStep, totpAt(59))
        assertNotEquals(firstStep, totpAt(60))
    }

    @Test
    fun `counter offset yields the neighbouring step`() {
        // T=59 → counter 1；+1 应等于 counter 2（T=60），-1 应等于 counter 0（T=0）
        assertEquals(totpAt(60), totpAt(59, counterOffset = 1))
        assertEquals(totpAt(0), totpAt(59, counterOffset = -1))
    }

    @Test
    fun `remaining millis counts down inside the step`() {
        assertEquals(30_000L, Totp.remainingMillis(0, 30))
        assertEquals(1_000L, Totp.remainingMillis(29_000, 30))
    }

    private fun totpAt(seconds: Long, digits: Int = 6, counterOffset: Int = 0): String =
        Totp.generate(
            secret = secret,
            timeMillis = seconds * 1000L,
            periodSeconds = 30,
            digits = digits,
            counterOffset = counterOffset,
        )
}
