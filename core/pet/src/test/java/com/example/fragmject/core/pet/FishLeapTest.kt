package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 跃起时间线的守卫测试。
 *
 * 锁死四条容易回归的性质，它们一旦破掉，表现都是"看不清但明显不对"：
 * 1. **预备段不放大、不出影子**（否则像"原地闪了一下"）；
 * 2. **滞空段有放大 + 影子分离**（"离开水面"的唯一硬线索）；
 * 3. **落水后压扁再回正**（重量感）；
 * 4. **时长结束后状态归零**，且只通知一次"结束"。
 */
class FishLeapTest {

    private val motion = FishMotion()
    private val water = FishWaterFx(headRadiusPx = 50f)
    private val leap = FishLeap(motion)

    private fun start(speed: Float = 0f, maxSpeed: Float = 350f) {
        leap.start(
            globalElapsed = 0f,
            center = Offset(100f, 100f),
            heading = 0f,
            speed = speed,
            maxSpeed = maxSpeed,
        )
        motion.middle = Offset(100f, 100f)
    }

    private fun advance(seconds: Float, step: Float = 1f / 60f) {
        var remaining = seconds
        while (remaining > 0f) {
            val dt = minOf(step, remaining)
            leap.update(dt, leap.elapsedSeconds + dt, water)
            leap.updateSplash(leap.elapsedSeconds)
            remaining -= dt
        }
    }

    @Test
    fun `未起跳时是关闭状态`() {
        assertFalse(leap.isActive)
        assertEquals(0f, leap.desiredSpeed(), 0.001f)
    }

    @Test
    fun `预备段不放大也不出影子`() {
        start()
        advance(0.05f)
        assertTrue(leap.isActive)
        assertEquals(1f, motion.leapScale, 0.001f)
        assertEquals(0f, motion.shadowAlpha, 0.001f)
        assertTrue("预备应压扁：${motion.stretchShort}", motion.stretchShort < 0.95f)
    }

    @Test
    fun `滞空段有放大与影子分离`() {
        start()
        advance(0.10f + 0.22f)
        assertTrue("应已放大：${motion.leapScale}", motion.leapScale > 1.05f)
        assertTrue("应有上抛：${motion.leapLiftR}", motion.leapLiftR > 0.1f)
        assertTrue("影子应分离：${motion.shadowAlpha}", motion.shadowAlpha > 0.05f)
        assertTrue("影子应偏移：${motion.shadowOffsetR}", motion.shadowOffsetR > 0.05f)
    }

    @Test
    fun `落水压扁后回正`() {
        val total = 0.10f + 0.44f + 0.14f
        start()
        advance(0.10f + 0.44f + 0.05f)
        assertTrue("落水应压扁：${motion.stretchShort}", motion.stretchShort < 0.95f)
        advance(total)
        assertEquals(1f, motion.stretchShort, 0.001f)
        assertEquals(1f, motion.stretchLong, 0.001f)
    }

    @Test
    fun `结束后只通知一次并关闭`() {
        start()
        advance(0.10f + 0.44f + 0.14f + 0.02f)
        assertFalse(leap.isActive)
        assertTrue(leap.consumeFinished())
        assertFalse("finished 应被消费掉", leap.consumeFinished())
        assertEquals(0f, motion.shadowAlpha, 0.001f)
        assertEquals(1f, motion.leapScale, 0.001f)
    }

    @Test
    fun `悬停起跳会自动补足起跳速度`() {
        // 到点悬停时速度约 0：直接沿用会变成"原地跳"，因此必须补到 1.15×巡航
        start(speed = 0f, maxSpeed = 200f)
        val launchSpeed = leap.desiredSpeed()
        assertEquals(230f, launchSpeed, 1f)
    }

    @Test
    fun `已在冲刺时保留原速度`() {
        start(speed = 400f, maxSpeed = 200f)
        assertEquals(400f, leap.desiredSpeed(), 1f)
    }
}
