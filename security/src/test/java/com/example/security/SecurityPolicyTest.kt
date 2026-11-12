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
    }
}
