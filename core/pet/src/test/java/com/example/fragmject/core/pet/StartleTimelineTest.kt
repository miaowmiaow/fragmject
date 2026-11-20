package com.example.fragmject.core.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 受惊时间线的守卫测试。
 *
 * 三段（僵直 / 逃逸 / 回头）此前是运行时里的四个私有常量，改一个值就可能让某一段
 * 悄悄消失——例如把 `GLANCE_START` 算错，"回头"会变成 0 秒，真机上要盯很久才发现。
 * 这里把阶段边界与各段速度倍率锁死。
 */
class StartleTimelineTest {

    private val freeze = StartleTimeline.FREEZE_SECONDS
    private val glanceStart = StartleTimeline.GLANCE_START_SECONDS
    private val total = StartleTimeline.TOTAL_SECONDS
    private val epsilon = 0.0001f

    @Test
    fun `三段按时间顺序排列且都不为零`() {
        assertTrue("僵直应为正", freeze > 0f)
        assertTrue("回头段应为正", StartleTimeline.GLANCE_SECONDS > 0f)
        assertTrue("回头起点应晚于僵直结束：$glanceStart", glanceStart > freeze)
        assertTrue("回头起点应早于整段结束：$glanceStart", glanceStart < total)
    }

    @Test
    fun `开头是僵直`() {
        assertEquals(StartlePhase.FREEZE, StartleTimeline.phaseAt(0f))
        assertEquals(StartlePhase.FREEZE, StartleTimeline.phaseAt(freeze - epsilon))
    }

    @Test
    fun `僵直结束后进入逃逸`() {
        assertEquals(StartlePhase.ESCAPE, StartleTimeline.phaseAt(freeze))
        assertEquals(StartlePhase.ESCAPE, StartleTimeline.phaseAt(glanceStart - epsilon))
    }

    @Test
    fun `最后一段是回头张望`() {
        assertEquals(StartlePhase.GLANCE, StartleTimeline.phaseAt(glanceStart))
        assertEquals(StartlePhase.GLANCE, StartleTimeline.phaseAt(total))
        // 超时（理论上不会被调用）也不该炸：归到回头段
        assertEquals(StartlePhase.GLANCE, StartleTimeline.phaseAt(total * 2f))
    }

    @Test
    fun `速度倍率符合预期`() {
        assertEquals(0f, StartleTimeline.speedFactorAt(StartlePhase.FREEZE), epsilon)
        assertEquals(1.9f, StartleTimeline.speedFactorAt(StartlePhase.ESCAPE), epsilon)
        assertEquals(0.35f, StartleTimeline.speedFactorAt(StartlePhase.GLANCE), epsilon)
    }

    @Test
    fun `回头起点等于总时长减回头段`() {
        assertEquals(total - StartleTimeline.GLANCE_SECONDS, glanceStart, epsilon)
    }
}
