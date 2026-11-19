package com.example.fragmject.convention

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * DEX 壳的**载荷打包协议**（纯逻辑，不依赖 Gradle / SDK，可在 JVM 单测中直接覆盖）。
 *
 * 与 `:shell` 的 `StubApplication` 严格对应，两侧任一处改动都必须同步：
 *
 * | 本文件 | 壳侧 |
 * |---|---|
 * | `encode(deflate(raw))` | `inflate(decode(chunk))` |
 * | `assets/payload/dex-<n>.bin` | 从 0 开始**连续**读取，遇到缺失即停 |
 * | `assets/payload/sum.txt`（对**密文**整体 SHA-256，小写 hex） | `verifyPayloadSum` |
 * | `assets/payload/shell.txt`（壳 dex CRC32，`%08x`） | `verifyShellDex` |
 * | `assets/payload/gadget.txt`、`lib/<abi>/libgadget*.so` | `maybeLoadGadgetProbe` |
 *
 * 顺序不可反：**先 deflate 再 XOR**。反过来载荷变成高熵数据、zip 压不动，
 * 实测壳包从 11.70 MB 涨到 15.20 MB。该约束由 `ShellPayloadTest` 的「解压后还原原文」用例兜底。
 *
 * 分工：[FragmjectDexShell] 的 `PackShellTask` 只负责路径解析、进程调用（zipalign / apksigner）
 * 与签名材料读取，字节层面的重排全部收敛在这里。
 */
internal object ShellPayload {

    /** 与壳侧 `XOR_BASE` 一致。 */
    const val XOR_BASE = 0x5A

    private const val SHELL_DEX_ENTRY = "classes.dex"
    private const val SUM_ENTRY = "assets/payload/sum.txt"
    private const val SHELL_ENTRY = "assets/payload/shell.txt"
    private const val GADGET_MARKER_ENTRY = "assets/payload/gadget.txt"
    private const val GADGET_SO_ENTRY = "lib/arm64-v8a/libgadget.so"
    private const val GADGET_CONFIG_ENTRY = "lib/arm64-v8a/libgadget.config.so"

    private val DEX_NAME_REGEX = Regex("classes\\d*\\.dex")

    /** 重签前必须从包里剔除的旧签名文件后缀（`META-INF/` 前缀另行判断）。 */
    private val SIGNATURE_SUFFIXES = listOf(".SF", ".RSA", ".DSA", ".EC", ".MF")

    /** 分片命名：壳按 `index` 从 0 连续读取，命名不能改。 */
    fun payloadEntryName(index: Int): String = "assets/payload/dex-$index.bin"

    /** 与壳侧 `decode` 对应：`data[i] ^ (XOR_BASE + i)`（自反）。 */
    fun encode(data: ByteArray): ByteArray = ByteArray(data.size) { i ->
        (data[i].toInt() xor (XOR_BASE + i)).toByte()
    }

    /** [encode] 的逆（同一算法），仅供测试与排障对照壳侧实现。 */
    fun decode(data: ByteArray): ByteArray = encode(data)

    /** 最大压缩率 deflate；壳侧用 `Inflater` 还原。 */
    fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream(data.size / 2)
        val buffer = ByteArray(64 * 1024)
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return out.toByteArray()
    }

    /** 与壳侧 `inflate` 对应（`Inflater` 逐块解压），仅供测试与排障。 */
    fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream(data.size * 3)
        val buffer = ByteArray(64 * 1024)
        while (!inflater.finished()) {
            val read = inflater.inflate(buffer)
            if (read == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
            } else {
                out.write(buffer, 0, read)
            }
        }
        inflater.end()
        return out.toByteArray()
    }

    /** 是否是需要剔除的旧签名文件。 */
    fun isSignatureFile(name: String): Boolean {
        val upper = name.uppercase()
        return upper.startsWith("META-INF/") && SIGNATURE_SUFFIXES.any { upper.endsWith(it) }
    }

    /** 对若干字节块整体求 SHA-256，小写 hex（与壳侧 `verifyPayloadSum` 的输出口径一致）。 */
    fun sha256Hex(chunks: List<ByteArray>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        chunks.forEach(digest::update)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** CRC32 的 8 位小写 hex（与壳侧 `shell.txt` 的解析口径一致）。 */
    fun crc32Hex(data: ByteArray): String =
        "%08x".format(CRC32().apply { update(data) }.value)

    /** Frida Gadget 探针载荷（仅测试包）；[configBytes] 为 null 表示缺少 `libgadget.config.so`。 */
    class GadgetPayload(val soBytes: ByteArray, val configBytes: ByteArray?)

    /** 重排结果，供任务打日志与单测断言。 */
    class RepackResult(
        val payloadChunks: Int,
        val payloadDigest: String,
        val shellCrc: String,
        /** manifest 里被剔除的 `android:appComponentFactory` 原值；null = 本来就没有。 */
        val removedAppComponentFactory: String? = null,
        /** manifest 改写失败的原因；null = 成功或无需改写（失败时保留原属性，不阻断打包）。 */
        val manifestPatchFailure: String? = null,
    )

    /**
     * 重排 APK：抽走全部 `classes*.dex` 写成加密载荷，塞入壳 dex，剔除旧签名文件。
     *
     * 同时剔除 manifest 的 `android:appComponentFactory`（见 [ShellManifest]）：
     * 该属性引用的类一定在载荷里，而系统在壳安装载荷之前就会解析它。
     *
     * 不做对齐与重签（依赖 build-tools，由任务负责）。
     */
    fun repack(
        source: File,
        target: File,
        shellDex: ByteArray,
        gadget: GadgetPayload? = null,
        warn: (String) -> Unit = {},
    ): RepackResult {
        val shellCrc = crc32Hex(shellDex)
        var payloadDigest = ""
        var payloadChunks = 0
        var removedAppComponentFactory: String? = null
        var manifestPatchFailure: String? = null

        ZipFile(source).use { zip ->
            val dexNames = zip.entries().asSequence()
                .map { it.name }
                .filter { DEX_NAME_REGEX.matches(it) }
                .toList()
            payloadChunks = dexNames.size

            ZipOutputStream(FileOutputStream(target)).use { out ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name !in dexNames }
                    .filter { !isSignatureFile(it.name) }
                    .forEach { entry ->
                        if (entry.name != ShellManifest.ENTRY) {
                            copyEntry(zip, entry, out)
                            return@forEach
                        }
                        val original = zip.getInputStream(entry).use { it.readBytes() }
                        when (val patched = ShellManifest.stripAppComponentFactory(original)) {
                            is ShellManifest.Result.Removed -> {
                                removedAppComponentFactory = patched.value
                                writeEntry(out, entry.name, patched.bytes)
                            }

                            ShellManifest.Result.Absent -> writeEntry(out, entry.name, original)

                            is ShellManifest.Result.Failed -> {
                                manifestPatchFailure = patched.reason
                                // warn 不 fail：壳仍可启动，只是每次冷启动多一条 ERROR 日志
                                warn("[shell] manifest 未改写（${patched.reason}），保留 appComponentFactory")
                                writeEntry(out, entry.name, original)
                            }
                        }
                    }

                // 顺序很关键：**先 deflate 再 XOR**（见类注释）
                val encodedChunks = dexNames.map { name ->
                    encode(deflate(zip.getInputStream(zip.getEntry(name)).use { it.readBytes() }))
                }
                encodedChunks.forEachIndexed { index, encoded ->
                    // 已经是压缩数据，用 STORED 避免 zip 再压一遍白耗 CPU
                    writeStored(out, payloadEntryName(index), encoded)
                }
                payloadDigest = sha256Hex(encodedChunks)
                writeEntry(out, SUM_ENTRY, payloadDigest.toByteArray(Charsets.UTF_8))

                // 壳 dex 自己的 CRC32：壳启动时读取并比对（防随手替换壳代码）。
                // 这是**同信任域**的校验（清单与壳 dex 同在 APK 内），
                // 真正的锚是签名校验；这里只提高「顺手改包」的成本。
                writeEntry(out, SHELL_ENTRY, shellCrc.toByteArray(Charsets.UTF_8))

                // Frida Gadget 探针（仅测试包）：生产构建不会带任何一项
                if (gadget != null) {
                    writeStored(out, GADGET_SO_ENTRY, gadget.soBytes)
                    val config = gadget.configBytes
                    if (config != null) {
                        writeStored(out, GADGET_CONFIG_ENTRY, config)
                    } else {
                        warn("[shell] 缺少 libgadget.config.so，gadget 可能阻塞启动")
                    }
                    writeEntry(out, GADGET_MARKER_ENTRY, "1".toByteArray(Charsets.UTF_8))
                }

                writeEntry(out, SHELL_DEX_ENTRY, shellDex)
            }
            return RepackResult(
                payloadChunks = payloadChunks,
                payloadDigest = payloadDigest,
                shellCrc = shellCrc,
                removedAppComponentFactory = removedAppComponentFactory,
                manifestPatchFailure = manifestPatchFailure,
            )
        }
    }

    private fun copyEntry(zip: ZipFile, entry: ZipEntry, out: ZipOutputStream) {
        val copy = ZipEntry(entry.name).apply {
            method = entry.method
            time = entry.time
            if (entry.method == ZipEntry.STORED) {
                size = entry.size
                crc = entry.crc
            }
        }
        out.putNextEntry(copy)
        zip.getInputStream(entry).use { it.copyTo(out) }
        out.closeEntry()
    }

    private fun writeEntry(out: ZipOutputStream, name: String, data: ByteArray) {
        out.putNextEntry(ZipEntry(name))
        out.write(data)
        out.closeEntry()
    }

    /** 以 STORED 方式写入（数据已压缩，避免二次压缩）。 */
    private fun writeStored(out: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name).apply {
            method = ZipEntry.STORED
            size = data.size.toLong()
            compressedSize = data.size.toLong()
            crc = CRC32().apply { update(data) }.value
        }
        out.putNextEntry(entry)
        out.write(data)
        out.closeEntry()
    }
}
