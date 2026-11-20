package com.example.fragmject.app.ui.pet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 穿越门控的守卫测试。
 *
 * 覆盖的是"静默失效"这一类问题：判定为 false 时鱼只是不穿越，不报错、不掉帧，
 * 真机上很容易被当成"偶发"。所以边界值（刚好 0ms、刚好 1000ms、1001ms、无点击）必须锁死。
 */
class TeleportGateTest {

    private val now = 100_000L

    @Test
    fun `从未点击过时不穿越`() {
        assertFalse(TeleportGate.shouldTeleport(now, TeleportGate.NO_TAP))
    }

    @Test
    fun `点击当帧触发切换时穿越`() {
        assertTrue(TeleportGate.shouldTeleport(now, tapAtMillis = now))
    }

    @Test
    fun `刚好等于新鲜度窗口仍然穿越`() {
        // 闭区间：回调可能恰好在 1000ms 边界上到达
        assertTrue(
            TeleportGate.shouldTeleport(now, tapAtMillis = now - TeleportGate.FRESHNESS_MS),
        )
    }

    @Test
    fun `超过新鲜度窗口不穿越`() {
        // 典型场景：返回手势 / 深链，没有新点击，只剩很久以前那次
        assertFalse(
            TeleportGate.shouldTeleport(now, tapAtMillis = now - TeleportGate.FRESHNESS_MS - 1),
        )
    }

    @Test
    fun `点击时间晚于当前时间视为异常，不穿越`() {
        // 两处取时钟不同源时会出现的脏数据：宁可不穿越，也不要按不可信的坐标瞬移
        assertFalse(TeleportGate.shouldTeleport(now, tapAtMillis = now + 16))
    }
}
