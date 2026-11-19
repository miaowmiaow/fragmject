package com.example.fragmject.convention

/**
 * 壳包 manifest 的最小二进制改写（AXML）。
 *
 * ============================ 为什么需要 ============================
 * `androidx.core:core` 的 manifest 会给宿主注入
 * `android:appComponentFactory="androidx.core.app.CoreComponentFactory"`。
 * 壳包把所有业务 dex（含 androidx.core）挪进 `assets/payload/`，而系统在
 * `LoadedApk.createAppFactory` 阶段——早于 `StubApplication.attachBaseContext`——
 * 就按 manifest 里的名字去加载该类，此时载荷尚未安装，必然
 * `ClassNotFoundException`，logcat 表现为
 * `E LoadedApk: Unable to instantiate appComponentFactory`。
 *
 * 该属性在壳包里没有任何可行指向：它引用的类**一定**在载荷内。因此打包时直接剔除，
 * 由系统回落默认的 `AppComponentFactory`。代价是壳包失去 `CompatWrapped` 包装能力
 * （本项目无实现方），普通包不受影响（本类只在 [ShellPayload.repack] 里被调用）。
 *
 * ============================ 做什么 ============================
 * 只处理一种情况：`<application>` 起始标签上的 `appComponentFactory` 属性。
 * 做法是删掉该属性记录（20 字节），回填属性计数与两级 chunk size，
 * 其余字节原样保留（含 string pool——被删的值仍留在池里，不影响运行）。
 *
 * 失败语义：**warn 不 fail**。AXML 是二进制格式，格式变化时宁可保留原属性
 * （壳仍可启动，只是多一条 ERROR 日志），也不能让打包链路直接失败。
 */
internal object ShellManifest {

    /** APK 内 manifest 的 entry 名。 */
    const val ENTRY = "AndroidManifest.xml"

    /** 需要剔除的属性名。 */
    const val ATTRIBUTE = "appComponentFactory"

    private const val TAG = "application"

    private const val RES_XML_TYPE = 0x0003
    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102

    /** `ResStringPool_header_flags`：UTF-8 池。 */
    private const val UTF8_FLAG = 0x0000_0100

    /** `ResXMLTree_attribute` 的最小长度（size 8 + res0 1 + dataType 1 + data 4 + 三个 string ref 12）。 */
    private const val ATTRIBUTE_MIN_SIZE = 20

    /** `ResXMLTree_attrExt` 的长度；`attributeStart` 通常就等于它。 */
    private const val ATTR_EXT_SIZE = 20

    private const val NO_ENTRY = -1

    /** 剔除结果。 */
    sealed class Result {

        /** 找到并删除；[value] 为原值（解析不到时为 null，仅影响日志）。 */
        class Removed(val bytes: ByteArray, val value: String?) : Result()

        /** manifest 本来就没有该属性（未来 AGP 不再注入时走这里）。 */
        object Absent : Result()

        /** AXML 结构不符合预期；调用方应保留原始字节并告警。 */
        class Failed(val reason: String) : Result()
    }

    /** 删除 `<application android:appComponentFactory=…>` 属性；不改动其余字节。 */
    fun stripAppComponentFactory(manifest: ByteArray): Result = try {
        strip(manifest)
    } catch (e: Exception) {
        Result.Failed(e.message ?: e.javaClass.simpleName)
    }

    private fun strip(src: ByteArray): Result {
        if (src.size < 8 || u16(src, 0) != RES_XML_TYPE) {
            return Result.Failed("不是 AXML（头部类型不匹配）")
        }
        val topSize = u32(src, 4)
        if (topSize > src.size || topSize < 8) {
            return Result.Failed("头部声明大小 $topSize 与实际 ${src.size} 不符")
        }

        var strings: List<String>? = null
        var pos = u16(src, 2)
        while (pos + 8 <= topSize) {
            val type = u16(src, pos)
            val headerSize = u16(src, pos + 2)
            val size = u32(src, pos + 4)
            if (size < 8 || pos + size > topSize) {
                return Result.Failed("chunk 越界 @$pos（size=$size）")
            }
            when (type) {
                RES_STRING_POOL_TYPE -> strings = readStringPool(src, pos)

                RES_XML_START_ELEMENT_TYPE -> {
                    val pool = strings ?: return Result.Failed("start element 出现在 string pool 之前")
                    val nameIndex = u32(src, pos + headerSize + 4)
                    if (pool.getOrNull(nameIndex) == TAG) {
                        // `<application>` 只出现一次：没有该属性即代表整个 manifest 没有，
                        // 直接返回；注意不能用 `continue`（会跳过 pos += size，死循环）
                        val found = findAttribute(src, pos, headerSize, pool) ?: return Result.Absent
                        return remove(src, pos, headerSize, found)
                    }
                }
            }
            pos += size
        }
        return Result.Absent
    }

    /** 找到的 `appComponentFactory` 属性：位置、长度与可读原值。 */
    private class Found(val offset: Int, val size: Int, val value: String?)

    private fun findAttribute(
        src: ByteArray,
        chunkStart: Int,
        headerSize: Int,
        pool: List<String>,
    ): Found? {
        val ext = chunkStart + headerSize
        val attributeStart = u16(src, ext + 8)
        val attributeSize = u16(src, ext + 10)
        val count = u16(src, ext + 12)
        if (attributeSize < ATTRIBUTE_MIN_SIZE) {
            throw IllegalArgumentException("attributeSize=$attributeSize 小于 $ATTRIBUTE_MIN_SIZE")
        }

        val attrs = ext + attributeStart
        for (index in 0 until count) {
            val offset = attrs + index * attributeSize
            if (offset + attributeSize > chunkStart + u32(src, chunkStart + 4)) {
                throw IllegalArgumentException("属性 $index 越出 start element 块")
            }
            if (pool.getOrNull(u32(src, offset + 4)) != ATTRIBUTE) continue

            val rawValue = u32(src, offset + 8)
            val value = when {
                rawValue != NO_ENTRY -> pool.getOrNull(rawValue)
                u8(src, offset + 15) == 0x03 -> pool.getOrNull(u32(src, offset + 16)) // TYPE_STRING
                else -> null
            }
            return Found(offset, attributeSize, value)
        }
        return null
    }

    /** 删掉一条属性记录，并回填 `attributeCount` 与两级 chunk 的 size。 */
    private fun remove(src: ByteArray, chunkStart: Int, headerSize: Int, found: Found): Result {
        val out = ByteArray(src.size - found.size)
        System.arraycopy(src, 0, out, 0, found.offset)
        System.arraycopy(src, found.offset + found.size, out, found.offset, src.size - found.offset - found.size)

        // 三处计数器都在被删记录之前，偏移在 out 中与原数组一致
        writeU32(out, 4, u32(src, 4) - found.size)
        writeU32(out, chunkStart + 4, u32(src, chunkStart + 4) - found.size)
        val ext = chunkStart + headerSize
        writeU16(out, ext + 12, u16(src, ext + 12) - 1)

        return Result.Removed(out, found.value)
    }

    // ------------------------------------------------------------------
    // AXML 基础结构
    // ------------------------------------------------------------------

    /**
     * 读取 string pool。
     *
     * 只支持 AXML 里出现的两种编码：UTF-8（`flags & 0x100`，aapt2 默认）与 UTF-16LE。
     * 两者都是「变长长度前缀 + 数据」，长度前缀的宽度由最高位决定。
     */
    private fun readStringPool(src: ByteArray, poolStart: Int): List<String> {
        val headerSize = u16(src, poolStart + 2)
        val count = u32(src, poolStart + 8)
        val flags = u32(src, poolStart + 16)
        val stringsStart = u32(src, poolStart + 20)
        val utf8 = flags and UTF8_FLAG != 0
        val offsetsBase = poolStart + headerSize
        if (count < 0 || offsetsBase + count * 4 > src.size) {
            throw IllegalArgumentException("string pool 条目数 $count 越界")
        }
        return (0 until count).map { index ->
            val offset = u32(src, offsetsBase + index * 4)
            val start = poolStart + stringsStart + offset
            if (utf8) decodeUtf8(src, start) else decodeUtf16(src, start)
        }
    }

    private fun decodeUtf8(src: ByteArray, start: Int): String {
        // 依次是「UTF-16 字符数」和「UTF-8 字节数」，都需要跳过
        var p = readLength(src, start).second
        val bytes = readLength(src, p)
        p = bytes.second
        return String(src, p, bytes.first, Charsets.UTF_8)
    }

    private fun decodeUtf16(src: ByteArray, start: Int): String {
        var length = u16(src, start)
        var p = start + 2
        if (length and 0x8000 != 0) {
            length = ((length and 0x7FFF) shl 16) or u16(src, p)
            p += 2
        }
        return String(src, p, length * 2, Charsets.UTF_16LE)
    }

    /** 读取 1~2 字节的变长长度，返回 (值, 下一个位置)。 */
    private fun readLength(src: ByteArray, offset: Int): Pair<Int, Int> {
        var p = offset
        var length = u8(src, p++)
        if (length and 0x80 != 0) {
            length = ((length and 0x7F) shl 8) or u8(src, p++)
        }
        return length to p
    }

    private fun u8(src: ByteArray, offset: Int): Int {
        require(offset in src.indices) { "读取越界 @$offset" }
        return src[offset].toInt() and 0xFF
    }

    private fun u16(src: ByteArray, offset: Int): Int {
        require(offset >= 0 && offset + 2 <= src.size) { "读取 u16 越界 @$offset" }
        return (src[offset].toInt() and 0xFF) or ((src[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun u32(src: ByteArray, offset: Int): Int {
        require(offset >= 0 && offset + 4 <= src.size) { "读取 u32 越界 @$offset" }
        return (src[offset].toInt() and 0xFF) or
            ((src[offset + 1].toInt() and 0xFF) shl 8) or
            ((src[offset + 2].toInt() and 0xFF) shl 16) or
            ((src[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun writeU16(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    private fun writeU32(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        target[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        target[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}
