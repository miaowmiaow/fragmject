package com.example.fragmject.convention

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.Random
import javax.tools.ToolProvider

/**
 * 打包端（[ShellPayload]，Kotlin）与壳端（`:shell` 的 `ShellCodec.java`）的**协议一致性**测试。
 *
 * 为什么需要它：两侧分属不同构建、不同语言，各写一份算法。任一侧改动都只会表现为
 * 「壳包能装但启动即崩」（`payload sum mismatch` 或 dex 解析失败），而且只有真机能发现。
 *
 * 做法：在测试运行时用 `javax.tools.JavaCompiler` 把 `:shell` 的 `ShellCodec.java`
 * 现编译到临时目录并加载，然后逐字节比对——**源码只有一份**，比对的是真实产物，
 * 不需要把 Java 源码复制进 build-logic。
 *
 * 前提：测试 JVM 必须是 JDK（Gradle 运行在 JDK 21 上）；源码路径由
 * `build.gradle.kts` 的 `systemProperty("fragmject.shellCodecSource", …)` 传入，
 * 路径失配时会直接失败而不是静默跳过。
 */
class ShellCodecParityTest {

    private val codec: Codec = Codec.load()

    @Test
    fun `XOR 基值两侧一致`() {
        assertEquals(
            ShellPayload.XOR_BASE,
            codec.clazz.getField("XOR_BASE").getInt(null),
        )
    }

    @Test
    fun `打包端 encode 与壳端 encode 字节完全一致`() {
        samples().forEach { (name, raw) ->
            assertArrayEquals(
                "样本[$name] deflate 不一致",
                ShellPayload.deflate(raw),
                codec.deflate(raw),
            )
            assertArrayEquals(
                "样本[$name] encode 不一致",
                ShellPayload.encode(ShellPayload.deflate(raw)),
                codec.encode(raw),
            )
        }
    }

    @Test
    fun `两端可互相解包`() {
        samples().forEach { (name, raw) ->
            // 壳端解打包端的产物
            assertArrayEquals(
                "样本[$name] 壳端无法解开打包端载荷",
                raw,
                codec.decode(ShellPayload.encode(ShellPayload.deflate(raw))),
            )
            // 打包端解壳端的产物（同一算法的自反性）
            assertArrayEquals(
                "样本[$name] 打包端无法解开壳端载荷",
                raw,
                ShellPayload.inflate(ShellPayload.decode(codec.encode(raw))),
            )
        }
    }

    /** 覆盖空载荷、单字节、高压缩率、随机字节与中文：前几类对应「实现差异」，随机字节卡住越界类 bug。 */
    private fun samples(): List<Pair<String, ByteArray>> {
        val random = ByteArray(64 * 1024).also { Random(42).nextBytes(it) }
        return listOf(
            "empty" to ByteArray(0),
            "single" to byteArrayOf(0x7F),
            "repeated" to ByteArray(4096) { 'A'.code.toByte() },
            "random" to random,
            "utf8-chinese" to "中文文案与符号 😀".toByteArray(Charsets.UTF_8),
        )
    }

    /** 反射包装：壳端只有静态方法，调用成本可忽略。 */
    private class Codec(val clazz: Class<*>) {

        fun encode(raw: ByteArray): ByteArray = invoke("encode", raw)

        fun decode(chunk: ByteArray): ByteArray = invoke("decode", chunk)

        fun deflate(raw: ByteArray): ByteArray = invoke("deflate", raw)

        fun inflate(compressed: ByteArray): ByteArray = invoke("inflate", compressed)

        private fun invoke(method: String, arg: ByteArray): ByteArray =
            clazz.getMethod(method, ByteArray::class.java).invoke(null, arg) as ByteArray

        companion object {
            fun load(): Codec {
                val source = locateSource()
                val compiler = ToolProvider.getSystemJavaCompiler()
                    ?: error("单测需要 JDK（javax.tools 不可用）：请用 JDK 21 运行 Gradle")
                val outDir = Files.createTempDirectory("shell-codec").toFile()
                // 诊断信息必须带进断言：编译失败时只报「expected 0 but was 1」等于没有信息
                val diagnostics = java.io.ByteArrayOutputStream()
                val exitCode = compiler.run(
                    null,
                    diagnostics,
                    diagnostics,
                    "-d",
                    outDir.absolutePath,
                    source.absolutePath,
                )
                assertEquals(
                    "ShellCodec.java 编译失败：${diagnostics.toString("UTF-8").trim()}",
                    0,
                    exitCode,
                )
                val loader = URLClassLoader(arrayOf(outDir.toURI().toURL()), Codec::class.java.classLoader)
                return Codec(loader.loadClass("com.example.shell.ShellCodec"))
            }

            private fun locateSource(): File {
                val candidates = listOfNotNull(
                    System.getProperty("fragmject.shellCodecSource")?.let(::File),
                    // 兜底：测试工作目录是 build-logic/convention
                    File("../../shell/src/main/java/com/example/shell/ShellCodec.java"),
                )
                val found = candidates.firstOrNull { it.isFile }
                assertTrue(
                    "找不到壳端协议源码，协议一致性无法校验（检查 build.gradle.kts 的 systemProperty）：$candidates",
                    found != null,
                )
                return found!!
            }
        }
    }
}
