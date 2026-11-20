package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 活动区域几何的守卫测试。
 *
 * 这些函数决定鱼会不会被画到容器外、受惊时是不是真的"贴边跑"，
 * 一旦回归（比如 clamp 边界写反）表现是「鱼贴着屏幕边缘被切掉一半」，肉眼很难定位。
 */
class FishSpaceTest {

    private val bounds = Size(1000f, 600f)
    private val margin = 50f

    @Test
    fun `clamp 把越界点收进安全区`() {
        assertEquals(Offset(margin, margin), FishSpace.clamp(Offset(-500f, -500f), bounds, margin))
        assertEquals(
            Offset(bounds.width - margin, bounds.height - margin),
            FishSpace.clamp(Offset(5000f, 5000f), bounds, margin),
        )
        val inside = Offset(300f, 300f)
        assertEquals(inside, FishSpace.clamp(inside, bounds, margin))
    }

    @Test
    fun `edgePoint 落在边界上且方向一致`() {
        val from = Offset(500f, 300f)
        val right = FishSpace.edgePoint(from, Offset(1f, 0f), bounds, margin)
        assertEquals(bounds.width - margin, right.x, 0.5f)
        assertEquals(from.y, right.y, 0.5f)

        val down = FishSpace.edgePoint(from, Offset(0f, 1f), bounds, margin)
        assertEquals(bounds.height - margin, down.y, 0.5f)
    }

    @Test
    fun `randomPoint 始终在安全区内`() {
        repeat(50) {
            val point = FishSpace.randomPoint(bounds, margin)
            assertTrue("x 越界：${point.x}", point.x in margin..(bounds.width - margin))
            assertTrue("y 越界：${point.y}", point.y in margin..(bounds.height - margin))
        }
    }

    @Test
    fun `软边界允许游出屏幕但不会彻底消失`() {
        val overshoot = 60f
        // 允许越过边缘
        val out = FishSpace.clampOutside(Offset(-40f, 700f), bounds, overshoot)
        assertEquals(-40f, out.x, 0.01f)
        assertEquals(bounds.height + overshoot, out.y, 0.01f)
        // 但越界有上限（否则鱼会跑到再也回不来的地方）
        val capped = FishSpace.clampOutside(Offset(-9999f, 9999f), bounds, overshoot)
        assertEquals(-overshoot, capped.x, 0.01f)
        assertEquals(bounds.height + overshoot, capped.y, 0.01f)
    }

    @Test
    fun `越界目标点落在屏幕外`() {
        val overshoot = 20f
        repeat(40) {
            val point = FishSpace.randomEdgeTarget(bounds, overshoot)
            val outside = point.x < 0f || point.y < 0f ||
                point.x > bounds.width || point.y > bounds.height
            assertTrue("目标点应落在屏幕外：$point", outside)
        }
    }

    @Test
    fun `isInside 只认屏幕矩形内`() {
        assertTrue(FishSpace.isInside(Offset(500f, 300f), bounds))
        assertTrue(FishSpace.isInside(Offset(0f, 0f), bounds))
        // 软边界允许游出去，此时中心点在矩形外：不该安排打盹/表演
        assertFalse(FishSpace.isInside(Offset(-1f, 300f), bounds))
        assertFalse(FishSpace.isInside(Offset(500f, bounds.height + 1f), bounds))
    }

    @Test
    fun `恢复的起点超出新容器时被夹回安全区`() {
        // 全屏时的坐标（1200×1600）在分屏后（1000×600）已经落在容器外，
        // 不夹紧的话鱼第一帧会被画到边外，只露半个身子。
        val restoredFromFullscreen = FishSpace.restoreStart(Offset(1200f, 1600f), bounds, margin)
        assertEquals(bounds.width - margin, restoredFromFullscreen.x, 0.01f)
        assertEquals(bounds.height - margin, restoredFromFullscreen.y, 0.01f)

        // 容器没变（切深色模式）时位置原样保留
        val inside = Offset(400f, 200f)
        assertEquals(inside, FishSpace.restoreStart(inside, bounds, margin))

        // 没有历史位置（首次出现）时用容器中心
        assertEquals(
            Offset(bounds.width / 2f, bounds.height / 2f),
            FishSpace.restoreStart(Offset.Zero, bounds, margin),
        )
    }

    @Test
    fun `随机方向是单位向量`() {
        repeat(20) {
            val direction = FishSpace.randomDirection()
            assertEquals(1f, direction.getDistance(), 0.001f)
        }
    }
}
