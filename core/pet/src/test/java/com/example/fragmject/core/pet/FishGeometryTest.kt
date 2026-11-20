package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 鱼几何的守卫测试。
 *
 * 移植时最大的风险不是画不出来，而是**坐标/角度口径搞反**（得到一条倒着游的鱼）。
 * 这几条用例把「0° 指向屏幕右侧」「90° 指向屏幕上方」「切线角 = 鱼头朝向」锁死，
 * 后续调整比例或换采样方式时不至于静默跑偏。
 */
class FishGeometryTest {

    private val delta = 0.001f

    @Test
    fun `0 度指向屏幕右侧且不改变 y`() {
        val start = Offset(10f, 20f)
        val end = FishGeometry.point(start, 100f, 0f)
        assertEquals(110f, end.x, delta)
        assertEquals(20f, end.y, delta)
    }

    @Test
    fun `90 度指向屏幕上方且长度不变`() {
        val start = Offset(0f, 200f)
        val end = FishGeometry.point(start, 100f, 90f)
        assertEquals(0f, end.x, delta)
        assertEquals(100f, end.y, delta)
        assertEquals(100f, (end - start).getDistance(), delta)
    }

    @Test
    fun `夹角按屏幕坐标系定向：目标在下方时为负`() {
        val angle = FishGeometry.includedAngle(
            center = Offset.Zero,
            head = Offset(1f, 0f),
            touch = Offset(0f, 1f),
        )
        assertEquals(-90f, angle, delta)
    }

    @Test
    fun `夹角在同向目标上为 0`() {
        val angle = FishGeometry.includedAngle(
            center = Offset.Zero,
            head = Offset(1f, 0f),
            touch = Offset(5f, 0f),
        )
        assertEquals(0f, angle, delta)
    }

    @Test
    fun `正前方目标时转弯控制点落在鱼头方向`() {
        val radius = 10f
        val middle = Offset.Zero
        val head = FishGeometry.point(middle, 1.6f * radius, 0f)
        val control = FishGeometry.turnControl(middle, head, Offset(500f, 0f), radius)
        assertEquals(head.x, control.x, delta)
        assertEquals(head.y, control.y, delta)
    }

    @Test
    fun `三次贝塞尔端点精确命中且中点对称`() {
        val p0 = Offset(0f, 0f)
        val p1 = Offset(0f, 10f)
        val p2 = Offset(10f, 10f)
        val p3 = Offset(10f, 0f)
        assertEquals(p0, FishGeometry.cubic(p0, p1, p2, p3, 0f))
        assertEquals(p3, FishGeometry.cubic(p0, p1, p2, p3, 1f))
        val middle = FishGeometry.cubic(p0, p1, p2, p3, 0.5f)
        assertEquals(5f, middle.x, delta)
        assertEquals(7.5f, middle.y, delta)
    }

    @Test
    fun `切线角与前进方向一致`() {
        val rightAngle = FishGeometry.cubicAngle(
            Offset(0f, 0f), Offset(1f, 0f), Offset(2f, 0f), Offset(3f, 0f), 0.5f,
        )
        assertEquals(0f, rightAngle, delta)

        val downAngle = FishGeometry.cubicAngle(
            Offset(0f, 0f), Offset(0f, 1f), Offset(0f, 2f), Offset(0f, 3f), 0.5f,
        )
        assertEquals(-90f, downAngle, delta)
    }

    @Test
    fun `角度归一到正负一百八十度`() {
        assertEquals(0f, FishGeometry.normalizeAngle(720f), delta)
        assertEquals(-170f, FishGeometry.normalizeAngle(190f), delta)
        assertEquals(170f, FishGeometry.normalizeAngle(-190f), delta)
        assertEquals(180f, FishGeometry.normalizeAngle(-180f), delta)
    }

    @Test
    fun `方向向量与朝向角互逆`() {
        for (heading in listOf(0f, 45f, 90f, -135f, 179f)) {
            val roundTrip = FishGeometry.headingOf(FishGeometry.direction(heading))
            assertEquals(0f, FishGeometry.normalizeAngle(roundTrip - heading), delta)
        }
        // 90° 指向屏幕上方（y 递减），与 point() 的口径一致
        val up = FishGeometry.direction(90f)
        assertEquals(0f, up.x, delta)
        assertEquals(-1f, up.y, delta)
    }

    @Test
    fun `控制点重合时不返回 NaN`() {
        val point = Offset(42f, 42f)
        val angle = FishGeometry.cubicAngle(point, point, point, point, 0.5f)
        assertTrue(angle.isFinite())
    }
}
