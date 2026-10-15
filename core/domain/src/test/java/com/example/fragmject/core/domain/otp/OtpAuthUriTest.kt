package com.example.fragmject.core.domain.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpAuthUriTest {

    @Test
    fun `parses standard totp uri`() {
        val uri = OtpAuthUri.parse(
            "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
        )
        requireNotNull(uri)
        assertEquals(OtpType.TOTP, uri.type)
        assertEquals("alice@example.com", uri.accountName)
        assertEquals("Example", uri.issuer)
        assertEquals("JBSWY3DPEHPK3PXP", uri.secret)
        assertEquals(OtpAlgorithm.SHA1, uri.algorithm)
        assertEquals(6, uri.digits)
        assertEquals(30, uri.period)
    }

    @Test
    fun `issuer falls back to the label prefix when parameter absent`() {
        val uri = OtpAuthUri.parse("otpauth://totp/ACME:bob@example.com?secret=JBSWY3DPEHPK3PXP")
        requireNotNull(uri)
        assertEquals("ACME", uri.issuer)
        assertEquals("bob@example.com", uri.accountName)
    }

    @Test
    fun `decodes percent encoded issuer and preserves plus in account`() {
        val uri = OtpAuthUri.parse(
            "otpauth://totp/%E8%85%BE%E8%AE%AF:alice%2Btag@example.com" +
                "?secret=JBSWY3DPEHPK3PXP&issuer=%E8%85%BE%E8%AE%AF",
        )
        requireNotNull(uri)
        assertEquals("alice+tag@example.com", uri.accountName)
        assertEquals("腾讯", uri.issuer)
    }

    @Test
    fun `parses explicit algorithm digits and period`() {
        val uri = OtpAuthUri.parse(
            "otpauth://totp/ACME:a@b.c?secret=JBSWY3DPEHPK3PXP" +
                "&algorithm=SHA256&digits=8&period=60",
        )
        requireNotNull(uri)
        assertEquals(OtpAlgorithm.SHA256, uri.algorithm)
        assertEquals(8, uri.digits)
        assertEquals(60, uri.period)
    }

    @Test
    fun `parses hotp with counter`() {
        val uri = OtpAuthUri.parse("otpauth://hotp/ACME:a@b.c?secret=JBSWY3DPEHPK3PXP&counter=7")
        requireNotNull(uri)
        assertEquals(OtpType.HOTP, uri.type)
        assertEquals(7L, uri.counter)
    }

    @Test
    fun `rejects invalid uris rather than importing a wrong account`() {
        listOf(
            "",
            "https://example.com",
            "otpauth://totp/ACME:a@b.c", // 缺 secret
            "otpauth://totp/ACME:a@b.c?secret=1111", // 非法 Base32
            "otpauth://totp/?secret=JBSWY3DPEHPK3PXP", // 缺账户名
            "otpauth://weird/ACME:a@b.c?secret=JBSWY3DPEHPK3PXP", // 未知类型
            "otpauth://totp/ACME:a@b.c?secret=JBSWY3DPEHPK3PXP&digits=9",
            "otpauth://totp/ACME:a@b.c?secret=JBSWY3DPEHPK3PXP&period=0",
        ).forEach { assertNull("should reject: $it", OtpAuthUri.parse(it)) }
    }

    @Test
    fun `generate and parse round trip including non ascii and plus`() {
        val source = OtpAuthUri(
            type = OtpType.TOTP,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "alice+tag@example.com",
            issuer = "腾讯",
            algorithm = OtpAlgorithm.SHA256,
            digits = 8,
            period = 60,
        )
        assertEquals(source, OtpAuthUri.parse(OtpAuthUri.generate(source)))
    }

    @Test
    fun `generate omits parameters equal to defaults`() {
        val uri = OtpAuthUri(
            type = OtpType.TOTP,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "a@b.c",
            issuer = "ACME",
        )
        assertEquals(
            "otpauth://totp/ACME:a@b.c?secret=JBSWY3DPEHPK3PXP&issuer=ACME",
            OtpAuthUri.generate(uri),
        )
    }

    @Test
    fun `generate emits counter for hotp`() {
        val uri = OtpAuthUri(
            type = OtpType.HOTP,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "a@b.c",
            counter = 7,
        )
        assertEquals(
            "otpauth://hotp/a@b.c?secret=JBSWY3DPEHPK3PXP&counter=7",
            OtpAuthUri.generate(uri),
        )
    }
}
