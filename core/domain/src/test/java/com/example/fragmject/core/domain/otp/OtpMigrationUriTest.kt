package com.example.fragmject.core.domain.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `otpauth-migration://` 批量导出格式解析测试。
 *
 * 载荷按 protobuf 线上格式**手工编码**（不依赖生成器），
 * 这样测试同时验证了 wire format 的正确性，而不是「自己写自己读」。
 */
class OtpMigrationUriTest {

    // ---- 手工 protobuf 编码工具 ----

    private fun varint(value: Long): ByteArray {
        var v = value
        val out = ArrayList<Byte>()
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) {
                out.add(b.toByte())
                break
            }
            out.add((b or 0x80).toByte())
        }
        return out.toByteArray()
    }

    private fun tag(field: Int, wireType: Int): ByteArray =
        varint(((field shl 3) or wireType).toLong())

    private fun lenDelimited(field: Int, payload: ByteArray): ByteArray =
        tag(field, 2) + varint(payload.size.toLong()) + payload

    private fun varintField(field: Int, value: Long): ByteArray = tag(field, 0) + varint(value)

    private fun otpParameters(
        secret: ByteArray,
        name: String = "",
        issuer: String = "",
        algorithm: Long = 1,
        digits: Long = 1,
        type: Long = 2,
        counter: Long = 0,
        trailingUnknownField: ByteArray = ByteArray(0),
    ): ByteArray =
        lenDelimited(1, secret) +
            (if (name.isEmpty()) ByteArray(0) else lenDelimited(2, name.toByteArray(Charsets.UTF_8))) +
            (if (issuer.isEmpty()) ByteArray(0) else lenDelimited(3, issuer.toByteArray(Charsets.UTF_8))) +
            varintField(4, algorithm) +
            varintField(5, digits) +
            varintField(6, type) +
            varintField(7, counter) +
            trailingUnknownField

    private fun migration(
        vararg accounts: ByteArray,
        extraFields: ByteArray = ByteArray(0),
    ): ByteArray = accounts.fold(ByteArray(0)) { acc, bytes -> acc + lenDelimited(1, bytes) } + extraFields

    private fun migrationUri(payload: ByteArray): String =
        "otpauth-migration://offline?data=" +
            java.util.Base64.getEncoder().encodeToString(payload)

    // ---- 正常解析 ----

    @Test
    fun `parses google style totp account`() {
        // RFC 6238 官方向量密钥的 ASCII 形态
        val secret = "12345678901234567890".toByteArray(Charsets.UTF_8)
        val uri = migrationUri(
            migration(
                otpParameters(
                    secret = secret,
                    name = "Example:alice@example.com",
                    issuer = "Example",
                    algorithm = 1,
                    digits = 1,
                    type = 2,
                ),
                extraFields = varintField(2, 1) + varintField(3, 0) + varintField(4, 1),
            ),
        )

        val accounts = OtpMigrationUri.parse(uri)

        assertEquals(1, accounts.size)
        val account = accounts.first()
        assertEquals("Example", account.issuer)
        assertEquals("alice@example.com", account.accountName)
        assertEquals(OtpType.TOTP, account.type)
        assertEquals(OtpAlgorithm.SHA1, account.algorithm)
        assertEquals(6, account.digits)
        assertEquals(0, account.counter)
        // secret 往返一致
        assertTrue(secret.contentEquals(Base32.decode(account.secret)))
        // 解析出的账户能直接算出 RFC 6238 官方向量验证码
        assertEquals("287082", Totp.generate(Base32.decode(account.secret)!!, 59_000L))
    }

    @Test
    fun `parses hotp with counter and eight digits`() {
        val uri = migrationUri(
            migration(
                otpParameters(
                    secret = ByteArray(10) { it.toByte() },
                    name = "GitHub:alice",
                    algorithm = 2, // SHA256
                    digits = 2,    // 8 位
                    type = 1,      // HOTP
                    counter = 5,
                ),
            ),
        )

        val account = OtpMigrationUri.parse(uri).single()

        assertEquals("GitHub", account.issuer)
        assertEquals("alice", account.accountName)
        assertEquals(OtpType.HOTP, account.type)
        assertEquals(OtpAlgorithm.SHA256, account.algorithm)
        assertEquals(8, account.digits)
        assertEquals(5, account.counter)
    }

    @Test
    fun `parses multiple accounts and skips unknown fields`() {
        val uri = migrationUri(
            migration(
                otpParameters(
                    secret = ByteArray(10) { (it + 1).toByte() },
                    name = "A:a1",
                ),
                otpParameters(
                    secret = ByteArray(10) { (it + 2).toByte() },
                    name = "B:a2",
                    // 未知字段（如未来新增的 pin=8）必须被跳过而不是解析失败
                    trailingUnknownField = lenDelimited(8, "ignored".toByteArray()),
                ),
                extraFields = varintField(2, 2) + varintField(3, 1) + varintField(4, 7) + varintField(9, 3),
            ),
        )

        val accounts = OtpMigrationUri.parse(uri)

        assertEquals(2, accounts.size)
        assertEquals("a1", accounts[0].accountName)
        assertEquals("a2", accounts[1].accountName)
    }

    @Test
    fun `accepts url-safe base64 and percent-encoded padding`() {
        val payload = migration(otpParameters(secret = ByteArray(20) { it.toByte() }, name = "A:a"))
        val standard = java.util.Base64.getEncoder().encodeToString(payload)
        val urlSafe = standard.replace('+', '-').replace('/', '_')
        val encoded = urlSafe.replace("=", "%3D")

        val accounts = OtpMigrationUri.parse("otpauth-migration://offline?data=$encoded")

        assertEquals(1, accounts.size)
        assertEquals("a", accounts.single().accountName)
    }

    @Test
    fun `label without colon is account name only`() {
        val uri = migrationUri(migration(otpParameters(secret = ByteArray(10) { 1 }, name = "plain-account")))

        val account = OtpMigrationUri.parse(uri).single()

        assertEquals("", account.issuer)
        assertEquals("plain-account", account.accountName)
        assertEquals("plain-account", account.displayIssuer)
    }

    // ---- 拒绝非法输入 ----

    @Test
    fun `rejects wrong scheme host and missing data`() {
        val payload = migration(otpParameters(secret = ByteArray(10) { 1 }, name = "A:a"))
        val data = java.util.Base64.getEncoder().encodeToString(payload)

        assertTrue(OtpMigrationUri.parse("otpauth://totp/A:a?secret=JBSWY3DPEHPK3PXP").isEmpty())
        assertTrue(OtpMigrationUri.parse("otpauth-migration://batch?data=$data").isEmpty())
        assertTrue(OtpMigrationUri.parse("otpauth-migration://offline").isEmpty())
        assertTrue(OtpMigrationUri.parse("otpauth-migration://offline?data=").isEmpty())
    }

    @Test
    fun `rejects corrupt base64 and truncated payload`() {
        // base64 非法字符
        assertTrue(OtpMigrationUri.parse("otpauth-migration://offline?data=!!!").isEmpty())

        // protobuf 声明的长度越界（varint 说 16384 字节但一个字节都没有）
        val truncated = lenDelimited(1, ByteArray(3)) + varint(16384)
        val uri = "otpauth-migration://offline?data=" +
            java.util.Base64.getEncoder().encodeToString(truncated)
        assertTrue(OtpMigrationUri.parse(uri).isEmpty())
    }

    @Test
    fun `drops accounts without secret`() {
        val uri = migrationUri(
            migration(
                otpParameters(secret = ByteArray(0), name = "A:no-secret"),
                otpParameters(secret = ByteArray(10) { 2 }, name = "B:ok"),
            ),
        )

        val accounts = OtpMigrationUri.parse(uri)

        assertEquals(1, accounts.size)
        assertEquals("ok", accounts.single().accountName)
    }

    @Test
    fun `drops accounts with blank name`() {
        val uri = migrationUri(migration(otpParameters(secret = ByteArray(10) { 3 }, name = "")))

        assertTrue(OtpMigrationUri.parse(uri).isEmpty())
    }

    // ---- 编码（导出）与往返 ----

    private fun account(index: Int, type: OtpType = OtpType.TOTP): OtpAccount = OtpAccount(
        id = index.toLong(),
        issuer = "Issuer$index",
        accountName = "user$index@example.com",
        secret = Base32.encode("secret-$index".toByteArray(Charsets.UTF_8)),
        algorithm = OtpAlgorithm.SHA1,
        digits = 6,
        type = type,
        counter = 7,
    )

    @Test
    fun `generate then parse round trips single account`() {
        val source = account(1)

        val uris = OtpMigrationUri.generate(listOf(source), batchId = 42)
        val parsed = uris.flatMap { OtpMigrationUri.parse(it) }

        assertEquals(1, uris.size)
        assertEquals(1, parsed.size)
        val restored = parsed.single()
        assertEquals(source.issuer, restored.issuer)
        assertEquals(source.accountName, restored.accountName)
        assertEquals(source.secret, restored.secret)
        assertEquals(source.algorithm, restored.algorithm)
        assertEquals(source.digits, restored.digits)
        assertEquals(source.type, restored.type)
        assertEquals(source.counter, restored.counter)
    }

    @Test
    fun `generate packs few accounts into one qr`() {
        // 真实量级：发行方 + 邮箱式账户名 + 20 字节密钥
        val sources = (1..3).map { account(it) }

        val uris = OtpMigrationUri.generate(sources, batchId = 1)

        assertEquals("少量账户必须合并成一张码", 1, uris.size)
    }

    @Test
    fun `generate then parse round trips hotp counter and eight digits`() {
        val source = account(2, type = OtpType.HOTP).copy(digits = 8, algorithm = OtpAlgorithm.SHA256)

        val restored = OtpMigrationUri.generate(listOf(source), batchId = 1)
            .flatMap { OtpMigrationUri.parse(it) }
            .single()

        assertEquals(OtpType.HOTP, restored.type)
        assertEquals(8, restored.digits)
        assertEquals(OtpAlgorithm.SHA256, restored.algorithm)
        assertEquals(source.counter, restored.counter)
    }

    @Test
    fun `generate splits into batches when payload exceeds one qr capacity`() {
        val sources = (1..40).map { account(it) }

        val uris = OtpMigrationUri.generate(sources, batchId = 7)

        assertTrue("账户多时应分成多张码", uris.size > 1)
        uris.forEach { uri ->
            val data = uri.substringAfter("data=")
            assertTrue(
                "单张码不得超过 ${OtpMigrationUri.MAX_PAYLOAD_CHARS} 字符：$data",
                data.length <= OtpMigrationUri.MAX_PAYLOAD_CHARS,
            )
        }
        // 所有批次合起来必须无遗漏、无重复，且保持导入顺序
        assertEquals(sources.map { it.accountName }, uris.flatMap { OtpMigrationUri.parse(it) }.map { it.accountName })
    }

    @Test
    fun `generate skips accounts with invalid secret and returns empty for empty input`() {
        val invalid = account(1).copy(secret = "not-base32!")

        assertTrue(OtpMigrationUri.generate(listOf(invalid), batchId = 1).isEmpty())
        assertTrue(OtpMigrationUri.generate(emptyList(), batchId = 1).isEmpty())
    }
}
