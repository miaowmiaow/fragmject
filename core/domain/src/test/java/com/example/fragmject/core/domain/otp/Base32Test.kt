package com.example.fragmject.core.domain.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Base32 编解码：使用 RFC 4648 测试向量（本实现不输出 `=` 填充）。 */
class Base32Test {

    @Test
    fun `encodes per RFC 4648 vectors without padding`() {
        assertEquals("MY", Base32.encode("f".toByteArray()))
        assertEquals("MZXQ", Base32.encode("fo".toByteArray()))
        assertEquals("MZXW6", Base32.encode("foo".toByteArray()))
        assertEquals("MZXW6YQ", Base32.encode("foob".toByteArray()))
        assertEquals("MZXW6YTB", Base32.encode("fooba".toByteArray()))
        assertEquals("MZXW6YTBOI", Base32.encode("foobar".toByteArray()))
    }

    @Test
    fun `decodes canonical form`() {
        assertArrayEquals("foobar".toByteArray(), Base32.decode("MZXW6YTBOI"))
    }

    @Test
    fun `decoding tolerates case spaces hyphens and padding`() {
        val expected = "foobar".toByteArray()
        assertArrayEquals(expected, Base32.decode("mzxw6ytboi"))
        assertArrayEquals(expected, Base32.decode("MZXW 6YTB OI"))
        assertArrayEquals(expected, Base32.decode("MZXW-6YTB-OI"))
        assertArrayEquals(expected, Base32.decode("MZXW6YTBOI======"))
    }

    @Test
    fun `rejects invalid input instead of silently ignoring it`() {
        assertNull(Base32.decode(""))
        assertNull(Base32.decode("MZXW6YTBO1")) // 1 不在 Base32 字母表
        assertNull(Base32.decode("MZXW6YTBO!"))
    }

    @Test
    fun `round trips arbitrary bytes`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x7F, 0x80.toByte(), 0xFF.toByte(), 0x2B)
        assertArrayEquals(bytes, Base32.decode(Base32.encode(bytes)))
    }

    @Test
    fun `secret used by authenticators is valid base32`() {
        assertArrayEquals(
            byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21, 0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()),
            Base32.decode("JBSWY3DPEHPK3PXP"),
        )
    }
}
