package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 转向行为的守卫测试。
 *
 * 「灵动」在实现上等价于三条可验证的性质：**转向有上限、速度有上限、到点会减速停下**。
 * 这三条任何一条破了，观感立刻退回「一顿一顿」或「绕着目标转圈」，
 * 所以这里不测画面，只测这三条不变量。
 */
class FishSteeringTest {

    private val tuning = FishSwimTuning(
        maxSpeed = 200f,
        maxTurnRate = 180f,
        acceleration = 600f,
        deceleration = 600f,
        slowRadius = 40f,
        arriveRadius = 5f,
        minSpeed = 10f,
    )

    private val frame = 1f / 60f

    @Test
    fun `单帧转向不超过最大角速度`() {
        val start = FishSwimState(position = Offset.Zero, heading = 0f, speed = 100f)
        // 目标在正下方（屏幕坐标 y 向下）→ 期望朝向 -90°，单帧 0.1s 最多转 18°
        val step = start.step(Offset(0f, 100f), dtSeconds = 0.1f, tuning = tuning)
        assertEquals(-18f, step.state.heading, 0.01f)
        assertEquals(-180f, step.turnRate, 0.01f)
    }

    @Test
    fun `转向取最短一侧`() {
        // 当前朝向 170°，目标朝向约 -170°：应继续顺时针转 +20° 到 -172°，
        // 而不是逆时针转 340°（那会让鱼原地甩头）
        val start = FishSwimState(position = Offset.Zero, heading = 170f, speed = 100f)
        val step = start.step(Offset(-1000f, 174f), dtSeconds = 0.1f, tuning = tuning)
        assertEquals(-172f, step.state.heading, 0.5f)
        assertTrue("转向方向反了：${step.turnRate}", step.turnRate > 0f)
    }

    @Test
    fun `多帧后朝向收敛到目标`() {
        var state = FishSwimState(position = Offset.Zero, heading = 0f, speed = 0f)
        val target = Offset(300f, 200f)
        repeat(300) {
            state = state.step(target, frame, tuning).state
        }
        val expected = FishGeometry.headingOf(target - state.position)
        val deviation = abs(FishGeometry.normalizeAngle(state.heading - expected))
        assertTrue("朝向偏差 $deviation°", deviation < 10f)
        assertTrue("离目标 ${(target - state.position).getDistance()}", (target - state.position).getDistance() < tuning.slowRadius)
    }

    @Test
    fun `速度不超过上限且不低于下限`() {
        var state = FishSwimState(position = Offset.Zero, heading = 0f, speed = 0f)
        repeat(240) {
            state = state.step(Offset(100000f, 0f), frame, tuning).state
            assertTrue("超速 ${state.speed}", state.speed <= tuning.maxSpeed + 0.01f)
        }
        assertTrue("低于巡航下限 ${state.speed}", state.speed >= tuning.minSpeed - 0.01f)
    }

    @Test
    fun `目标就是自身位置时判定 arrived 并把速度降到 0`() {
        // 对应 hover：驱动侧把 target 设成鱼的当前位置，鱼应停在原地只摆尾
        var state = FishSwimState(position = Offset(48f, 0f), heading = 30f, speed = 120f)
        var arrived = false
        repeat(120) {
            val step = state.step(state.position, frame, tuning)
            state = step.state
            arrived = arrived || step.arrived
            // 悬停中朝向不该被 atan2(0,0) 拉向 0°
            assertEquals(30f, state.heading, 0.01f)
        }
        assertTrue("从未判定到达", arrived)
        assertEquals(0f, state.speed, 0.01f)
    }

    @Test
    fun `不会冲过目标来回打转`() {
        val target = Offset(300f, 0f)
        var state = FishSwimState(position = Offset.Zero, heading = 0f, speed = 0f)
        var maxOvershoot = 0f
        repeat(600) {
            state = state.step(target, frame, tuning).state
            maxOvershoot = maxOf(maxOvershoot, state.position.x - target.x)
        }
        // 减速距离 = v²/2a = 200²/1200 ≈ 33px，远小于 slowRadius 40，理论上不该冲过
        assertTrue("冲过目标 ${maxOvershoot}px", maxOvershoot < 5f)
        assertEquals(300f, state.position.x, tuning.slowRadius)
        assertEquals(0f, state.position.y, 1f)
    }

    @Test
    fun `dt 为 0 时状态不变`() {
        val start = FishSwimState(position = Offset(10f, 20f), heading = 30f, speed = 80f)
        val step = start.step(Offset(500f, 500f), dtSeconds = 0f, tuning = tuning)
        assertEquals(start, step.state)
    }
}
