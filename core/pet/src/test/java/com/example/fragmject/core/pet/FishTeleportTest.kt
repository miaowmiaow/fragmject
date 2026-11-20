package com.example.fragmject.core.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 穿越时间轴的守卫测试。
 *
 * 锁死三条容易回归的性质，破掉后的表现都是"看着怪但说不出哪怪"：
 * 1. **换位前必须完全不可见**（`alpha == 0`），否则用户会看到鱼"闪一下跳过去"；
 * 2. **淡入要有回弹**（峰值 > 1），否则出现得没有弹性、像贴图淡入；
 * 3. **结束后必须回到 1.0**，否则鱼会一直半透明。
 */
class FishTeleportTest {

    private val fadeOut = FishTeleport.FADE_OUT_SECONDS
    private val arrive = FishTeleport.ARRIVE_AT_SECONDS
    private val total = FishTeleport.TOTAL_SECONDS

    @Test
    fun `不在穿越中时透明度与缩放都为 1`() {
        assertEquals(1f, FishTeleport.alpha(-1f), 0.001f)
        assertEquals(1f, FishTeleport.scale(-1f), 0.001f)
    }

    @Test
    fun `淡出段透明度线性下降`() {
        assertEquals(1f, FishTeleport.alpha(0f), 0.001f)
        assertEquals(0.5f, FishTeleport.alpha(fadeOut / 2f), 0.05f)
        assertEquals(0f, FishTeleport.alpha(fadeOut), 0.001f)
    }

    @Test
    fun `换位置的窗口内完全不可见`() {
        // 这是整个交互成立的前提：跳变只能发生在看不见的时候
        assertEquals(0f, FishTeleport.alpha(fadeOut + 0.001f), 0.001f)
        assertEquals(0f, FishTeleport.alpha(arrive - 0.001f), 0.001f)
        assertTrue(FishTeleport.hasArrived(arrive))
        assertFalse(FishTeleport.hasArrived(arrive - 0.01f))
    }

    @Test
    fun `淡入带回弹且最终回到 1`() {
        val peakAt = arrive + FishTeleport.FADE_IN_SECONDS * 0.6f
        assertTrue("应有过冲：${FishTeleport.scale(peakAt)}", FishTeleport.scale(peakAt) > 1.05f)
        assertEquals(1f, FishTeleport.alpha(total), 0.001f)
        assertEquals(1f, FishTeleport.scale(total), 0.001f)
    }

    @Test
    fun `淡出时收缩到最小值`() {
        assertEquals(0.72f, FishTeleport.scale(fadeOut), 0.001f)
        assertEquals(0.72f, FishTeleport.scale(arrive - 0.001f), 0.001f)
    }

    @Test
    fun `结束判定在总时长处`() {
        assertFalse(FishTeleport.hasFinished(total - 0.01f))
        assertTrue(FishTeleport.hasFinished(total))
    }
}
