package com.example.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 策略层的纯逻辑测试：退避间隔、WARN 去重键、配置默认值。
 *
 * 为什么单独一个文件：`SecurityDetectorsTest` 覆盖「判定」，这里覆盖「策略」。
 * [SecurityEngine] 本身依赖 Handler/Looper，无法在 JVM 里直接测，
 * 因此把可决定的逻辑抽成纯函数（[nextIntervalMs]）与纯属性（[SecurityHit.dedupeKey]）。
 */
class SecurityPolicyTest {

    @Test
    fun `未达到退避阈值时间隔不变`() {
        val config = SecurityConfig(cleanRoundsToBackoff = 3, maxIntervalMs = 60_000)

        assertEquals(1_000L, nextIntervalMs(1_000L, cleanRounds = 1, config = config))
        assertEquals(1_000L, nextIntervalMs(1_000L, cleanRounds = 2, config = config))
    }

    @Test
    fun `达到退避阈值时间隔翻倍`() {
        val config = SecurityConfig(cleanRoundsToBackoff = 3, maxIntervalMs = 60_000)

        assertEquals(2_000L, nextIntervalMs(1_000L, cleanRounds = 3, config = config))
        assertEquals(4_000L, nextIntervalMs(2_000L, cleanRounds = 3, config = config))
    }

    @Test
    fun `间隔不会超过上限`() {
        val config = SecurityConfig(cleanRoundsToBackoff = 1, maxIntervalMs = 8_000)

        assertEquals(8_000L, nextIntervalMs(6_000L, cleanRounds = 1, config = config))
        assertEquals(8_000L, nextIntervalMs(8_000L, cleanRounds = 1, config = config))
    }

    @Test
    fun `去重键忽略原因里的运行期数字`() {
        val first = SecurityHit("检测到异常匿名可执行内存段（2 段，阈值 1）", Severity.CRITICAL)
        val second = SecurityHit("检测到异常匿名可执行内存段（5 段，阈值 1）", Severity.CRITICAL)

        // 段数变化不应导致「去重失效、日志重复刷屏」
        assertEquals(first.dedupeKey, second.dedupeKey)
        assertEquals("检测到异常匿名可执行内存段", first.dedupeKey)
    }

    @Test
    fun `没有括号的原因原样作为去重键`() {
        val hit = SecurityHit("检测到 Root 环境", Severity.WARN)

        assertEquals("检测到 Root 环境", hit.dedupeKey)
    }

    @Test
    fun `连续失败达到阈值才降级`() {
        assertFalse(shouldDegrade(failures = 1, limit = 3, alreadyDegraded = false))
        assertFalse(shouldDegrade(failures = 2, limit = 3, alreadyDegraded = false))
        assertTrue(shouldDegrade(failures = 3, limit = 3, alreadyDegraded = false))
        // 已降级不重复触发（避免重复上报 / 重复日志）
        assertFalse(shouldDegrade(failures = 5, limit = 3, alreadyDegraded = true))
        // limit <= 0：不降级（显式要求「命中即阻断」的宿主）
        assertFalse(shouldDegrade(failures = 99, limit = 0, alreadyDegraded = false))
    }

    @Test
    fun `事件序列化为单行 JSON 且正确转义`() {
        val event = SecurityEvent(
            code = "injection.anon_segment",
            reason = "检测到 \"异常\" 段\n换行",
            severity = Severity.CRITICAL,
            timestampMillis = 1700000000000L,
            detail = mapOf("segments" to "2", "threshold" to "1"),
        )

        val json = event.toJson()

        assertEquals(
            "{\"code\":\"injection.anon_segment\"," +
                "\"reason\":\"检测到 \\\"异常\\\" 段\\n换行\"," +
                "\"severity\":\"CRITICAL\"," +
                "\"timestamp\":1700000000000," +
                "\"detail\":{\"segments\":\"2\",\"threshold\":\"1\"}}",
            json,
        )
        // 单行：JSON 里不应出现裸换行（必须是转义后的 \n）
        assertFalse(json.contains('\n'))
    }

    @Test
    fun `限流出口在窗口内丢弃超额事件`() {
        val received = mutableListOf<SecurityEvent>()
        val sink = ThrottledEventSink(
            delegate = object : SecurityEventSink {
                override fun emit(event: SecurityEvent) {
                    received += event
                }
            },
            maxEventsPerWindow = 2,
            windowMs = 1_000L,
        )

        repeat(5) { index -> sink.emit(eventAt(index * 100L)) }
        assertEquals(2, received.size)

        // 窗口滑过后重新放行
        sink.emit(eventAt(2_000L))
        assertEquals(3, received.size)
    }

    @Test
    fun `过滤出口只放行指定 code`() {
        val received = mutableListOf<String>()
        val sink = FilteredEventSink(
            delegate = object : SecurityEventSink {
                override fun emit(event: SecurityEvent) {
                    received += event.code
                }
            },
            codes = setOf("integrity.signature"),
        )

        sink.emit(eventAt(0L, code = "env.root"))
        sink.emit(eventAt(1L, code = "integrity.signature"))

        assertEquals(listOf("integrity.signature"), received)
    }

    private fun eventAt(millis: Long, code: String = "env.root") = SecurityEvent(
        code = code,
        reason = "测试",
        severity = Severity.WARN,
        timestampMillis = millis,
    )

    @Test
    fun `默认配置不跳过检测且匿名段阈值留出容忍度`() {
        val config = SecurityConfig()

        // 生产包默认不跳过；跳过只由宿主编译期传入的 debugBypass 决定
        assertFalse(config.debugBypass)
        // 默认 1（大于才判定）：曾因 ART JIT 段名变更误杀，留一段容忍
        assertEquals(1, config.anonExecThreshold)
        // 加密校验默认不启用（空集合 = 不校验），避免未配置的宿主全量误杀
        assertEquals(emptySet<String>(), config.expectedSignatureSha256)
        // 反 dump 默认开启：收紧 /proc 权限是「做比不做好」的动作，且有配置可回滚
        assertTrue(config.antiDump)
        // 检测项默认启用 DEFAULT（= 全部 - PLT_HOOK）：PLT 检测因误报风险默认关
        assertEquals(SecurityCheck.DEFAULT, config.checks)
        assertFalse(SecurityCheck.PLT_HOOK in config.checks)
        // 诊断模式默认关：避免线上多打日志
        assertFalse(config.diagnosticMode)
    }
}
