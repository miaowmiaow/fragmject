package com.example.fragmject.convention

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 壳包 manifest 改写测试（纯字节，不依赖 aapt2 / SDK）。
 *
 * 覆盖 `ShellManifest.stripAppComponentFactory` 的三条路径与 `repack` 的集成点：
 * 删除后属性计数与两级 chunk size 必须同步回填，否则 manifest 会被系统判为损坏
 * （装机报 `INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION`）；解析失败时必须原样保留。
 */
class ShellManifestTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `剔除 appComponentFactory 并回填属性计数与块大小`() {
        val manifest = manifestWith(
            attr("appComponentFactory", string = "androidx.core.app.CoreComponentFactory"),
            attr("allowBackup", int = 1),
        )

        val result = ShellManifest.stripAppComponentFactory(manifest)

        assertTrue("应命中 Removed，实际 $result", result is ShellManifest.Result.Removed)
        result as ShellManifest.Result.Removed
        assertEquals("androidx.core.app.CoreComponentFactory", result.value)
        assertEquals("删掉一条 20 字节属性记录", manifest.size - 20, result.bytes.size)

        // 回填三处计数器：顶层 XML chunk、start element chunk、attrExt.attributeCount
        assertEquals(u32(manifest, 4) - 20, u32(result.bytes, 4))
        assertEquals(u32(manifest, startElementOffset(manifest) + 4) - 20, u32(result.bytes, startElementOffset(result.bytes) + 4))
        assertEquals(1, u16(result.bytes, startElementOffset(result.bytes) + 16 + 12))

        // 剩下的是 allowBackup，且尾部（end element）字节未被移动
        val attrs = startElementOffset(result.bytes) + 36
        assertEquals(2, u32(result.bytes, attrs + 4)) // "allowBackup" 的字符串索引
        assertArrayEquals(manifest.takeLast(24).toByteArray(), result.bytes.takeLast(24).toByteArray())

        // 幂等：再跑一次应判定为「没有该属性」
        assertTrue(ShellManifest.stripAppComponentFactory(result.bytes) is ShellManifest.Result.Absent)
    }

    @Test
    fun `没有该属性时返回 Absent 且不改字节`() {
        val manifest = manifestWith(attr("allowBackup", int = 1))

        assertTrue(ShellManifest.stripAppComponentFactory(manifest) is ShellManifest.Result.Absent)
    }

    @Test
    fun `UTF-16 字符串池同样能解析出原值`() {
        val manifest = manifestWith(
            utf16 = true,
            attributes = arrayOf(attr("appComponentFactory", string = "androidx.core.app.CoreComponentFactory")),
        )

        val result = ShellManifest.stripAppComponentFactory(manifest)

        assertTrue(result is ShellManifest.Result.Removed)
        assertEquals("androidx.core.app.CoreComponentFactory", (result as ShellManifest.Result.Removed).value)
    }

    @Test
    fun `非法字节不抛异常而是 Failed`() {
        assertTrue(ShellManifest.stripAppComponentFactory(ByteArray(0)) is ShellManifest.Result.Failed)
        assertTrue(ShellManifest.stripAppComponentFactory(ByteArray(16) { 0x7F }) is ShellManifest.Result.Failed)
        assertTrue(ShellManifest.stripAppComponentFactory("<manifest/>".toByteArray()) is ShellManifest.Result.Failed)
    }

    @Test
    fun `repack 输出的 manifest 已不含该属性`() {
        val manifest = manifestWith(attr("appComponentFactory", string = "androidx.core.app.CoreComponentFactory"))
        val apk = File(tmp.root, "in.apk").apply {
            ZipOutputStream(outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write(manifest)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("classes.dex"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
        }
        val warnings = mutableListOf<String>()
        val out = File(tmp.root, "shell.apk")

        val result = ShellPayload.repack(
            source = apk,
            target = out,
            shellDex = byteArrayOf(4),
            warn = { warnings.add(it) },
        )

        assertEquals("androidx.core.app.CoreComponentFactory", result.removedAppComponentFactory)
        assertNull(result.manifestPatchFailure)
        assertTrue("成功改写时不应告警：$warnings", warnings.isEmpty())

        ZipFile(out).use { zip ->
            val patched = zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readBytes() }
            assertTrue(ShellManifest.stripAppComponentFactory(patched) is ShellManifest.Result.Absent)
        }
    }

    @Test
    fun `manifest 无法解析时告警但保留原始字节`() {
        val raw = "<manifest/>".toByteArray()
        val apk = File(tmp.root, "raw.apk").apply {
            ZipOutputStream(outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write(raw)
                zip.closeEntry()
            }
        }
        val warnings = mutableListOf<String>()
        val out = File(tmp.root, "raw-shell.apk")

        val result = ShellPayload.repack(apk, out, byteArrayOf(1), warn = { warnings.add(it) })

        assertNull(result.removedAppComponentFactory)
        assertTrue(result.manifestPatchFailure != null)
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("appComponentFactory"))
        ZipFile(out).use { zip ->
            assertArrayEquals(raw, zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readBytes() })
        }
    }

    // ------------------------------------------------------------------
    // 最小 AXML 构造器（只覆盖 `<application>` 起始标签场景）
    // ------------------------------------------------------------------

    private class Attribute(
        val name: String,
        val stringValue: String? = null,
        val intValue: Int? = null,
    )

    private fun attr(name: String, string: String? = null, int: Int? = null): Attribute {
        require((string == null) != (int == null)) { "属性值二选一" }
        return Attribute(name, string, int)
    }

    private fun manifestWith(vararg attributes: Attribute): ByteArray =
        manifestWith(utf16 = false, attributes = attributes)

    private fun manifestWith(utf16: Boolean, attributes: Array<out Attribute>): ByteArray {
        val strings = buildList {
            add("application")                              // 0：元素名
            attributes.forEach { add(it.name) }
            attributes.forEach { it.stringValue?.let(::add) }
        }
        val pool = stringPool(strings, utf16)

        val startElement = startElement(
            nameIndex = 0,
            attributes = attributes.map { attribute ->
                val nameIndex = strings.indexOf(attribute.name)
                val value = attribute.stringValue
                Triple(
                    nameIndex,
                    value?.let(strings::indexOf) ?: -1,
                    attribute.intValue,
                )
            },
            utf16 = utf16,
        )
        val endElement = endElement(nameIndex = 0)

        val total = 8 + pool.size + startElement.size + endElement.size
        val out = ByteArray(total)
        write16(out, 0, 0x0003) // RES_XML_TYPE
        write16(out, 2, 8)
        write32(out, 4, total)
        System.arraycopy(pool, 0, out, 8, pool.size)
        System.arraycopy(startElement, 0, out, 8 + pool.size, startElement.size)
        System.arraycopy(endElement, 0, out, 8 + pool.size + startElement.size, endElement.size)
        return out
    }

    private fun startElement(
        nameIndex: Int,
        attributes: List<Triple<Int, Int, Int?>>,
        utf16: Boolean,
    ): ByteArray {
        val nodeHeader = 16
        val attrExt = 20
        val attrSize = 20
        val total = nodeHeader + attrExt + attributes.size * attrSize
        val out = ByteArray(total)

        write16(out, 0, 0x0102) // RES_XML_START_ELEMENT_TYPE
        write16(out, 2, nodeHeader)
        write32(out, 4, total)
        write32(out, 8, 1)                        // lineNumber
        write32(out, 12, -1)                      // comment
        // ResXMLTree_attrExt
        write32(out, 16, -1)                      // ns
        write32(out, 20, nameIndex)
        write16(out, 24, attrExt)                 // attributeStart
        write16(out, 26, attrSize)                // attributeSize
        write16(out, 28, attributes.size)         // attributeCount
        write16(out, 30, 0)
        write16(out, 32, 0)
        write16(out, 34, 0)

        var offset = nodeHeader + attrExt
        attributes.forEach { (name, rawValue, intValue) ->
            write32(out, offset, -1)              // ns
            write32(out, offset + 4, name)
            write32(out, offset + 8, if (rawValue >= 0) rawValue else -1)
            write16(out, offset + 12, 8)          // typedValue.size
            out[offset + 15] = (if (intValue != null) 0x10 else 0x03).toByte() // TYPE_INT_DEC / TYPE_STRING
            write32(out, offset + 16, intValue ?: rawValue)
            offset += attrSize
        }
        return out
    }

    private fun endElement(nameIndex: Int): ByteArray {
        val out = ByteArray(24)
        write16(out, 0, 0x0103) // RES_XML_END_ELEMENT_TYPE
        write16(out, 2, 16)
        write32(out, 4, out.size)
        write32(out, 8, 1)
        write32(out, 12, -1)
        write32(out, 16, -1)
        write32(out, 20, nameIndex)
        return out
    }

    private fun stringPool(strings: List<String>, utf16: Boolean): ByteArray {
        val encoded = strings.map { encodeString(it, utf16) }
        val headerSize = 28
        val dataStart = headerSize + strings.size * 4
        val dataSize = encoded.sumOf { it.size }
        val total = dataStart + dataSize + (4 - dataSize % 4) % 4
        val out = ByteArray(total)

        write16(out, 0, 0x0001) // RES_STRING_POOL_TYPE
        write16(out, 2, headerSize)
        write32(out, 4, total)
        write32(out, 8, strings.size)
        write32(out, 12, 0)                        // styleCount
        write32(out, 16, if (utf16) 0 else 0x100)  // flags：UTF8_FLAG
        write32(out, 20, dataStart)                // stringsStart
        write32(out, 24, 0)                        // stylesStart

        var dataOffset = 0
        encoded.forEachIndexed { index, bytes ->
            write32(out, headerSize + index * 4, dataOffset)
            System.arraycopy(bytes, 0, out, dataStart + dataOffset, bytes.size)
            dataOffset += bytes.size
        }
        return out
    }

    private fun encodeString(value: String, utf16: Boolean): ByteArray {
        if (utf16) {
            val chars = value.toCharArray()
            val out = ByteArray(2 + chars.size * 2 + 2)
            write16(out, 0, chars.size)
            chars.forEachIndexed { index, c -> write16(out, 2 + index * 2, c.code) }
            return out
        }
        val utf8 = value.toByteArray(Charsets.UTF_8)
        require(utf8.size < 0x80 && value.length < 0x80) { "测试数据只用短 ASCII 串" }
        return byteArrayOf(value.length.toByte(), utf8.size.toByte()) + utf8 + byteArrayOf(0)
    }

    /** 找到 `<application>` 起始标签 chunk 的偏移（顶层头之后的第二个 chunk）。 */
    private fun startElementOffset(manifest: ByteArray): Int {
        var pos = 8
        while (pos + 8 <= manifest.size) {
            if (u16(manifest, pos) == 0x0102) return pos
            pos += u32(manifest, pos + 4)
        }
        error("没有 start element")
    }

    private fun u16(src: ByteArray, offset: Int): Int =
        (src[offset].toInt() and 0xFF) or ((src[offset + 1].toInt() and 0xFF) shl 8)

    private fun u32(src: ByteArray, offset: Int): Int =
        (src[offset].toInt() and 0xFF) or
            ((src[offset + 1].toInt() and 0xFF) shl 8) or
            ((src[offset + 2].toInt() and 0xFF) shl 16) or
            ((src[offset + 3].toInt() and 0xFF) shl 24)

    private fun write16(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    private fun write32(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        target[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        target[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}
