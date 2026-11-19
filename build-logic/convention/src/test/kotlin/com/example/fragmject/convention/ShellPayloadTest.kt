package com.example.fragmject.convention

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * DEX 壳载荷协议测试（不依赖 Android SDK / d8 / apksigner）。
 *
 * 覆盖 `repack` 的字节层面契约，这些点错了会产出「能装但启动即崩」或「装不上」的包：
 * 业务 dex 被抽走、分片顺序与壳侧连续读取一致、`sum.txt` 覆盖全部密文分片、
 * `shell.txt` 与壳 dex 的 CRC 对应、旧签名文件被剔除、STORED entry 不丢 size/crc。
 *
 * 对齐与重签（zipalign / apksigner）依赖 build-tools，仍由真机与 release-weekly CI 验收。
 */
class ShellPayloadTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ---------------- 编解码协议 ----------------

    @Test
    fun `载荷编解码与壳侧口径一致`() {
        val raw = ByteArray(512) { (it * 7).toByte() }
        val packed = ShellPayload.encode(ShellPayload.deflate(raw))
        // 壳侧顺序：先 decode 再 inflate
        assertArrayEquals(raw, ShellPayload.inflate(ShellPayload.decode(packed)))
    }

    @Test
    fun `先 deflate 再 XOR 确实比直接 XOR 更小`() {
        val compressible = ByteArray(4096) { 'A'.code.toByte() }
        val deflatedFirst = ShellPayload.encode(ShellPayload.deflate(compressible)).size
        val xorFirst = ShellPayload.deflate(ShellPayload.encode(compressible)).size
        assertTrue(
            "deflate 前 XOR 会让数据变高熵（实测 +30% 体积）",
            deflatedFirst < xorFirst,
        )
    }

    @Test
    fun `签名文件判定覆盖常见后缀`() {
        listOf(
            "META-INF/CERT.SF",
            "META-INF/CERT.RSA",
            "META-INF/CERT.DSA",
            "META-INF/CERT.EC",
            "META-INF/MANIFEST.MF",
        ).forEach { assertTrue(it, ShellPayload.isSignatureFile(it)) }
        assertTrue(ShellPayload.isSignatureFile("meta-inf/cert.sf"))
        // 非签名资源不能被误删（ServiceLoader 声明、普通资产）
        assertFalse(ShellPayload.isSignatureFile("META-INF/services/foo"))
        assertFalse(ShellPayload.isSignatureFile("assets/a.MF"))
    }

    // ---------------- repack ----------------

    @Test
    fun `repack 抽走业务 dex 并写入可还原的分片`() {
        val dex0 = "dex-zero".toByteArray()
        val dex1 = "dex-one-longer".toByteArray()
        val apk = buildApk(
            listOf(
                apkEntry("AndroidManifest.xml", "<manifest/>".toByteArray()),
                apkEntry("classes.dex", dex0),
                apkEntry("res/layout/a.xml", "<x/>".toByteArray()),
                apkEntry("classes2.dex", dex1),
                apkEntry("META-INF/CERT.SF", "sig".toByteArray()),
                apkEntry("META-INF/MANIFEST.MF", "mf".toByteArray()),
            ),
        )
        val shellDex = byteArrayOf(0x64, 0x65, 0x78)
        val out = File(tmp.root, "shell.apk")

        val result = ShellPayload.repack(apk, out, shellDex)

        assertEquals(2, result.payloadChunks)
        ZipFile(out).use { zip ->
            val names = zip.names()
            assertFalse("业务 dex 必须被抽走", names.contains("classes2.dex"))
            assertTrue(names.contains("AndroidManifest.xml"))
            assertTrue(names.contains("res/layout/a.xml"))
            assertFalse("旧签名文件必须剔除", names.any { it.startsWith("META-INF/") })
            assertArrayEquals(shellDex, zip.read("classes.dex"))

            // 分片顺序 = 原 zip 中 dex 的出现顺序；解出来必须与原文一致
            val chunk0 = zip.read("assets/payload/dex-0.bin")
            val chunk1 = zip.read("assets/payload/dex-1.bin")
            assertArrayEquals(dex0, ShellPayload.inflate(ShellPayload.decode(chunk0)))
            assertArrayEquals(dex1, ShellPayload.inflate(ShellPayload.decode(chunk1)))

            // sum.txt 覆盖全部**密文**分片（顺序敏感）
            val expectedSum = sha256Hex(chunk0, chunk1)
            assertEquals(expectedSum, String(zip.read("assets/payload/sum.txt")))
            assertEquals(result.payloadDigest, expectedSum)

            // shell.txt = 壳 dex 的 CRC32（小写 8 位）
            val expectedCrc = "%08x".format(CRC32().apply { update(shellDex) }.value)
            assertEquals(expectedCrc, String(zip.read("assets/payload/shell.txt")))
            assertEquals(result.shellCrc, expectedCrc)
        }
    }

    @Test
    fun `分片以 STORED 写入且保留原有条目的压缩方式`() {
        val apk = buildApk(
            listOf(
                apkEntry("classes.dex", "dex".toByteArray()),
                apkEntry("res/raw.bin", "stored-entry".toByteArray(), stored = true),
                apkEntry("assets/text.txt", "deflated-entry".toByteArray()),
            ),
        )
        val out = File(tmp.root, "methods.apk")
        ShellPayload.repack(apk, out, byteArrayOf(1))

        ZipFile(out).use { zip ->
            // 载荷已是压缩数据，再用 DEFLATED 只会白耗 CPU
            assertEquals(ZipEntry.STORED, zip.getEntry("assets/payload/dex-0.bin").method)
            // 原样搬运：STORED 仍是 STORED，DEFLATED 仍是 DEFLATED
            assertEquals(ZipEntry.STORED, zip.getEntry("res/raw.bin").method)
            assertEquals(ZipEntry.DEFLATED, zip.getEntry("assets/text.txt").method)
            assertEquals("stored-entry", String(zip.read("res/raw.bin")))
        }
    }

    @Test
    fun `没有业务 dex 时仍有壳 dex 与空摘要`() {
        val apk = buildApk(listOf(apkEntry("AndroidManifest.xml", "<m/>".toByteArray())))
        val out = File(tmp.root, "nodex.apk")
        val result = ShellPayload.repack(apk, out, byteArrayOf(9, 9))

        assertEquals(0, result.payloadChunks)
        ZipFile(out).use { zip ->
            assertTrue(zip.names().contains("classes.dex"))
            assertFalse(zip.names().any { it.startsWith("assets/payload/dex-") })
            // 空分片集合的 SHA-256（壳侧同样会算这一次）
            assertEquals(sha256Hex(), String(zip.read("assets/payload/sum.txt")))
        }
    }

    // ---------------- gadget 探针 ----------------

    @Test
    fun `gadget 探针写入 so、配置与 marker`() {
        val out = File(tmp.root, "gadget.apk")
        ShellPayload.repack(
            source = buildApk(listOf(apkEntry("classes.dex", "d".toByteArray()))),
            target = out,
            shellDex = byteArrayOf(1),
            gadget = ShellPayload.GadgetPayload(byteArrayOf(2, 3), byteArrayOf(4)),
        )

        ZipFile(out).use { zip ->
            assertArrayEquals(byteArrayOf(2, 3), zip.read("lib/arm64-v8a/libgadget.so"))
            assertArrayEquals(byteArrayOf(4), zip.read("lib/arm64-v8a/libgadget.config.so"))
            assertEquals("1", String(zip.read("assets/payload/gadget.txt")))
        }
    }

    @Test
    fun `缺少 gadget 配置时告警但仍写 marker`() {
        val warnings = mutableListOf<String>()
        val out = File(tmp.root, "gadget-noconfig.apk")
        ShellPayload.repack(
            source = buildApk(listOf(apkEntry("classes.dex", "d".toByteArray()))),
            target = out,
            shellDex = byteArrayOf(1),
            gadget = ShellPayload.GadgetPayload(byteArrayOf(2), null),
            warn = { warnings.add(it) },
        )

        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("libgadget.config.so"))
        ZipFile(out).use { zip ->
            assertNull(zip.getEntry("lib/arm64-v8a/libgadget.config.so"))
            // marker 仍要写：壳据此决定是否 loadLibrary("gadget")
            assertTrue(zip.names().contains("assets/payload/gadget.txt"))
        }
    }

    @Test
    fun `未开探针时不写任何 gadget 资源`() {
        val warnings = mutableListOf<String>()
        val out = File(tmp.root, "nogadget.apk")
        ShellPayload.repack(
            source = buildApk(listOf(apkEntry("classes.dex", "d".toByteArray()))),
            target = out,
            shellDex = byteArrayOf(1),
            warn = { warnings.add(it) },
        )

        assertTrue(warnings.isEmpty())
        ZipFile(out).use { zip ->
            assertTrue(zip.names().none { it.startsWith("lib/") })
            assertFalse(zip.names().contains("assets/payload/gadget.txt"))
        }
    }

    // ---------------- 工具 ----------------

    private data class ApkEntry(val name: String, val data: ByteArray, val stored: Boolean = false)

    private fun apkEntry(name: String, data: ByteArray, stored: Boolean = false): ApkEntry =
        ApkEntry(name, data, stored)

    private fun buildApk(entries: List<ApkEntry>): File {
        val file = File(tmp.root, "input-${System.nanoTime()}.apk")
        ZipOutputStream(file.outputStream()).use { out ->
            entries.forEach { entry ->
                val zipEntry = ZipEntry(entry.name)
                if (entry.stored) {
                    zipEntry.method = ZipEntry.STORED
                    zipEntry.size = entry.data.size.toLong()
                    zipEntry.compressedSize = entry.data.size.toLong()
                    zipEntry.crc = CRC32().apply { update(entry.data) }.value
                }
                out.putNextEntry(zipEntry)
                out.write(entry.data)
                out.closeEntry()
            }
        }
        return file
    }

    private fun ZipFile.names(): List<String> =
        entries().asSequence().filter { !it.isDirectory }.map { it.name }.toList()

    private fun ZipFile.read(name: String): ByteArray =
        getInputStream(getEntry(name)).use { it.readBytes() }

    private fun sha256Hex(vararg chunks: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        chunks.forEach(digest::update)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
