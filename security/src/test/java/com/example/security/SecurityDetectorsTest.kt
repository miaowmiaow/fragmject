package com.example.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Test

/**
 * 检测链的纯 JVM 单测。
 *
 * 之所以能这样测：检测项只依赖 [SecurityFacts] / [NativeChecks] 两个接口，
 * 不直接读 `Build.*`、不直接碰 `File`。这层抽象是「库化前置改造」的产物——
 * 拆出前这些分支只能靠真机刷 su/模拟器手工验证。
 *
 * H5 之后额外校验「严重级别」：Root / 模拟器默认 WARN（不杀进程），
 * 明确攻击特征固定 CRITICAL；级别由 [SecurityConfig] 的 [Action] 决定。
 */
class SecurityDetectorsTest {

    @Test
    fun `native tracerPid 命中时优先于 Java 调试判定`() {
        val facts = FakeFacts().apply { debugger = true }
        val native = FakeNative().apply { tracerPid = 4242 }

        val hit = detectHit(facts, native, SecurityConfig(), includePortScan = false, includeCrc = false)

        assertTrue(hit?.reason.orEmpty().contains("TracerPid=4242"))
        assertEquals(Severity.CRITICAL, hit?.severity)
    }

    @Test
    fun `Java 调试器命中且为 CRITICAL`() {
        val hit = detectHit(FakeFacts().apply { debugger = true }, FakeNative(), SecurityConfig(), false, false)

        assertEquals("检测到 Java 层调试器（JDWP）", hit?.reason)
        assertEquals(Severity.CRITICAL, hit?.severity)
    }

    @Test
    fun `Frida 特征线程命中`() {
        // H3 之后线程名匹配在 native，Kotlin 只拿命中数
        val native = FakeNative().apply { fridaThreads = 1 }

        val hit = detectHit(FakeFacts(), native, SecurityConfig(), false, false)

        assertEquals("检测到 Frida 特征线程", hit?.reason)
        assertEquals(Severity.CRITICAL, hit?.severity)
    }

    @Test
    fun `Frida 落地文件命中`() {
        val native = FakeNative().apply { fridaFiles = 1 }

        val hit = detectHit(FakeFacts(), native, SecurityConfig(), false, false)

        assertEquals("检测到 Frida 落地文件", hit?.reason)
    }

    @Test
    fun `匿名可执行段大于阈值才命中`() {
        val facts = FakeFacts()
        val native = FakeNative().apply { anonymousSegments = 1 }

        assertNull(detectHit(facts, native, SecurityConfig(anonExecThreshold = 1), false, false))

        native.anonymousSegments = 2
        val hit = detectHit(facts, native, SecurityConfig(anonExecThreshold = 1), false, false)
        // 命中原因带实际段数与阈值：线上误杀时靠这两个数字调参
        assertTrue(hit?.reason.orEmpty().contains("2 段"))
        assertTrue(hit?.reason.orEmpty().contains("阈值 1"))
        assertEquals(Severity.CRITICAL, hit?.severity)
    }

    @Test
    fun `Root 默认 WARN`() {
        val native = FakeNative().apply { rootFiles = 1 }

        val hit = detectHit(FakeFacts(), native, SecurityConfig(), false, false)

        assertEquals("检测到 Root 环境", hit?.reason)
        assertEquals(Severity.WARN, hit?.severity)
    }

    @Test
    fun `Root 配置 BLOCK 后升级为 CRITICAL`() {
        val native = FakeNative().apply { rootFiles = 1 }

        val hit = detectHit(FakeFacts(), native, SecurityConfig(rootAction = Action.BLOCK), false, false)

        assertEquals(Severity.CRITICAL, hit?.severity)
    }

    @Test
    fun `Root 配置 IGNORE 后完全不检测`() {
        val native = FakeNative().apply { rootFiles = 1 }

        assertNull(detectHit(FakeFacts(), native, SecurityConfig(rootAction = Action.IGNORE), false, false))
    }

    @Test
    fun `Root 管理应用命中`() {
        val facts = FakeFacts().apply { installed += "com.topjohnwu.magisk" }

        assertEquals("检测到 Root 环境", detectHit(facts, FakeNative(), SecurityConfig(), false, false)?.reason)
    }

    @Test
    fun `模拟器需要两个独立信号且默认 WARN`() {
        val single = FakeFacts().apply { build = baseBuild.copy(hardware = "ranchu") }
        assertNull(detectHit(single, FakeNative(), SecurityConfig(), false, false))

        val double = FakeFacts().apply {
            build = baseBuild.copy(hardware = "ranchu", product = "sdk_gphone64_arm64")
        }
        val hit = detectHit(double, FakeNative(), SecurityConfig(), false, false)
        assertEquals("检测到模拟器环境", hit?.reason)
        assertEquals(Severity.WARN, hit?.severity)
    }

    @Test
    fun `模拟器配置 BLOCK 后升级为 CRITICAL`() {
        val facts = FakeFacts().apply {
            build = baseBuild.copy(hardware = "ranchu", product = "sdk_gphone64_arm64")
        }

        val hit = detectHit(facts, FakeNative(), SecurityConfig(emulatorAction = Action.BLOCK), false, false)

        assertEquals(Severity.CRITICAL, hit?.severity)
    }

    @Test
    fun `模拟器配置 IGNORE 后不命中`() {
        val facts = FakeFacts().apply {
            build = baseBuild.copy(hardware = "ranchu", product = "sdk_gphone64_arm64")
        }

        assertNull(
            detectHit(facts, FakeNative(), SecurityConfig(emulatorAction = Action.IGNORE), false, false),
        )
    }

    @Test
    fun `CRITICAL 优先于 WARN`() {
        // 同时存在 Root（WARN）与 Frida 线程（CRITICAL）：必须报 CRITICAL
        val native = FakeNative().apply {
            rootFiles = 1
            fridaThreads = 1
        }

        val hit = detectHit(FakeFacts(), native, SecurityConfig(), false, false)

        assertEquals("检测到 Frida 特征线程", hit?.reason)
        assertEquals(Severity.CRITICAL, hit?.severity)
    }

    @Test
    fun `端口扫描仅在 includePortScan 为 true 时执行`() {
        val facts = FakeFacts().apply { openPorts += 27042 }

        assertNull(detectHit(facts, FakeNative(), SecurityConfig(), includePortScan = false, includeCrc = false))
        assertEquals(
            "检测到 Frida Server 监听端口（27042-27052）",
            detectHit(facts, FakeNative(), SecurityConfig(), includePortScan = true, includeCrc = false)?.reason,
        )
    }

    @Test
    fun `libc CRC 不一致命中且只在 includeCrc 时执行`() {
        val native = FakeNative().apply { libcCrc = 1 }

        assertNull(detectHit(FakeFacts(), native, SecurityConfig(), false, false))
        assertEquals(
            "检测到 libc 代码段被篡改（疑似 inline hook）",
            detectHit(FakeFacts(), native, SecurityConfig(), false, true)?.reason,
        )
    }

    @Test
    fun `关闭 INJECTION 后不再检测 Frida`() {
        val native = FakeNative().apply { fridaThreads = 1 }
        val config = SecurityConfig(checks = SecurityCheck.ALL - SecurityCheck.INJECTION)

        assertNull(detectHit(FakeFacts(), native, config, false, false))
    }

    @Test
    fun `关闭 ROOT 与 EMULATOR 后环境类完全不检测`() {
        val native = FakeNative().apply { rootFiles = 1 }
        val config = SecurityConfig(
            checks = SecurityCheck.ALL - SecurityCheck.ROOT - SecurityCheck.EMULATOR,
        )

        assertNull(detectHit(FakeFacts(), native, config, false, false))
    }

    @Test
    fun `命中带稳定 code`() {
        val native = FakeNative().apply { tracerPid = 42 }

        val hit = detectHit(FakeFacts(), native, SecurityConfig(), false, false)

        assertEquals("debugger.native", hit?.code)
    }

    @Test
    fun `默认不启用 PLT_HOOK 检测`() {
        val native = FakeNative().apply { pltHooks = 3 }

        // 默认 checks 不含 PLT_HOOK：即使 native 报劫持也不命中
        assertNull(detectHit(FakeFacts(), native, SecurityConfig(), false, true))
    }

    @Test
    fun `启用 PLT_HOOK 后劫持命中`() {
        val native = FakeNative().apply { pltHooks = 3 }
        val config = SecurityConfig(checks = SecurityCheck.ALL)

        val hit = detectHit(FakeFacts(), native, config, false, true)

        assertEquals("integrity.plt_hook", hit?.code)
    }

    @Test
    fun `PLT 检查未到期或无法解析时放行`() {
        val native = FakeNative().apply { pltHooks = 3 }
        val config = SecurityConfig(checks = SecurityCheck.ALL)

        // includeCrc=false：本轮不做该检查
        assertNull(detectHit(FakeFacts(), native, config, false, false))
        // 无法解析（-1）：放行
        native.pltHooks = -1
        assertNull(detectHit(FakeFacts(), native, config, false, true))
    }

    @Test
    fun `干净环境不命中`() {
        assertNull(detectHit(FakeFacts(), FakeNative(), SecurityConfig(), true, true))
    }

    @Test
    fun `native 不可用时不影响 JVM 侧检测`() {
        val native = FakeNative(available = false).apply { tracerPid = 999 }

        assertNull(detectHit(FakeFacts(), native, SecurityConfig(), false, false))
    }

    @Test
    fun `签名未配置时放行`() {
        assertTrue(verifySignature(FakeFacts(), SecurityConfig()))
    }

    @Test
    fun `签名匹配忽略大小写与冒号`() {
        val facts = FakeFacts().apply { signatures = listOf("AABBCCDD") }

        assertTrue(verifySignature(facts, SecurityConfig(expectedSignatureSha256 = setOf("aa:bb:cc:dd"))))
    }

    @Test
    fun `签名不匹配时失败`() {
        val facts = FakeFacts().apply { signatures = listOf("AABBCCDD") }

        assertFalse(verifySignature(facts, SecurityConfig(expectedSignatureSha256 = setOf("11223344"))))
    }

    @Test
    fun `签名查询失败按失败处理`() {
        assertFalse(verifySignature(FakeFacts(), SecurityConfig(expectedSignatureSha256 = setOf("11223344"))))
    }

    @Test
    fun `资源基线为空时放行`() {
        assertTrue(verifyResourceIntegrity(FakeFacts(), SecurityConfig()))
    }

    @Test
    fun `资源摘要不匹配时失败`() {
        val facts = FakeFacts().apply { apkEntries["resources.arsc"] = "0011" }

        assertFalse(
            verifyResourceIntegrity(facts, SecurityConfig(expectedResourceSha256 = mapOf("resources.arsc" to "2233"))),
        )
        assertTrue(
            verifyResourceIntegrity(facts, SecurityConfig(expectedResourceSha256 = mapOf("resources.arsc" to "00:11"))),
        )
    }

    @Test
    fun `native 不可用时跳过自身 so 校验`() {
        val soFile = File("dummy.so")
        assertTrue(verifySelfSoIntegrity(FakeNative(available = false), soFile))
        // .rodata 是第二道独立校验，降级语义必须一致
        assertTrue(verifySelfSoDataIntegrity(FakeNative(available = false), soFile))
        // 解压失败（soFile == null）同样按降级放行
        assertTrue(verifySelfSoIntegrity(FakeNative(available = true), null))
    }

    private companion object {
        /** 一台普通真机的 Build 特征（不含任何模拟器关键字）。 */
        val baseBuild = BuildValues(
            fingerprint = "google/sunfish/sunfish:13/tq3a.230805.001/10316531:user/release-keys",
            model = "pixel 4a",
            product = "sunfish",
            hardware = "sunfish",
            brand = "google",
            manufacturer = "google",
        )
    }
}

/** 可控的假环境事实。 */
private class FakeFacts : SecurityFacts {

    var debugger = false
    var waiting = false
    var build: BuildValues = BuildValues("", "", "", "", "", "")
    var signatures: List<String> = emptyList()

    val files = mutableSetOf<String>()
    val texts = mutableMapOf<String, String>()
    val installed = mutableSetOf<String>()
    val openPorts = mutableSetOf<Int>()
    val apkEntries = mutableMapOf<String, String>()

    override fun isDebuggerConnected(): Boolean = debugger

    override fun waitingForDebugger(): Boolean = waiting

    override fun exists(path: String): Boolean = path in files

    override fun readText(path: String): String? = texts[path]

    override fun buildValues(): BuildValues = build

    override fun isPackageInstalled(pkg: String): Boolean = pkg in installed

    override fun isPortOpen(port: Int, timeoutMs: Int): Boolean = port in openPorts

    override fun packageSignatureSha256(): List<String> = signatures

    override fun apkPath(): String = "/data/app/base.apk"

    override fun cacheDirPath(): String = "/data/user/0/pkg/cache"

    override fun lastUpdateTimeMillis(): Long = 0L

    override fun supportedAbi(): String? = "arm64-v8a"

    override fun copyApkEntry(entryName: String, targetPath: String): Boolean = false

    override fun apkEntrySha256(entryName: String): String? = apkEntries[entryName]
}

/** 可控的假 native 层。 */
private class FakeNative(override var available: Boolean = true) : NativeChecks {

    var tracerPid = 0
    var fridaMaps = 0
    var fridaThreads = 0
    var fridaFiles = 0
    var rootMaps = 0
    var rootFiles = 0
    var anonymousSegments = 0
    var libcCrc = 0
    var selfSoCrc = 0
    var pltHooks = 0

    override fun load(): Boolean = available

    override fun installAntiPtrace(): Int = 0

    override fun readTracerPid(): Int = tracerPid

    override fun countAnonymousExecSegments(): Int = anonymousSegments

    override fun findFridaInMaps(): Int = fridaMaps

    override fun findRootInMaps(): Int = rootMaps

    override fun findFridaThreads(): Int = fridaThreads

    override fun findFridaFiles(): Int = fridaFiles

    override fun findRootFiles(): Int = rootFiles

    override fun hardenProcess(): Int = 0

    override fun countPltHooks(): Int = pltHooks

    override fun verifyLibcTextCrc(): Int = libcCrc

    override fun verifySelfSoTextCrc(selfSoPath: String): Int = selfSoCrc

    override fun verifySelfSoDataCrc(selfSoPath: String): Int = selfSoCrc

    override fun killSelf() = Unit
}
