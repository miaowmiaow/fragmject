package com.example.fragmject.app.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 事件中枢的组装契约：id 自增、时间戳取自注入时钟、穿越只在「1s 内有点击」时产生。
 *
 * 时钟注入是这些断言成立的前提（`SystemClock` 在 JVM 单测里拿不到真实时间，写死在实现里
 * 就只能靠真机验证）。
 */
class PetInputHubTest {

    private var now = 1_000L
    private val hub = PetInputHub(clock = { now })

    @Test
    fun `tap increments id and stamps the injected clock`() {
        hub.emitTap(Offset(10f, 10f))
        hub.emitTap(Offset(20f, 20f))

        assertEquals(2, hub.tap?.id)
        assertEquals(Offset(20f, 20f), hub.tap?.position)
        assertEquals(1_000L, hub.tap?.atMillis)
    }

    @Test
    fun `swipe increments id independently of tap`() {
        hub.emitTap(Offset.Zero)
        hub.emitSwipe(Offset(5f, 5f))
        hub.emitSwipe(Offset(6f, 6f))

        assertEquals(1, hub.tap?.id)
        assertEquals(2, hub.swipe?.id)
    }

    @Test
    fun `route change without any tap does not teleport`() {
        hub.notifyRouteChanged()

        assertNull(hub.teleport)
    }

    @Test
    fun `route change right after a tap teleports to that tap`() {
        val position = Offset(120f, 640f)
        hub.emitTap(position)
        now = 1_500L
        hub.notifyRouteChanged()

        assertNotNull(hub.teleport)
        assertEquals(position, hub.teleport?.position)
    }

    @Test
    fun `stale tap is ignored by the freshness gate`() {
        hub.emitTap(Offset(1f, 1f))
        now = 1_000L + TeleportGate.FRESHNESS_MS + 1
        hub.notifyRouteChanged()

        assertNull(hub.teleport)
    }

    @Test
    fun `each route change that passes the gate gets a new teleport id`() {
        hub.emitTap(Offset(30f, 30f))
        hub.notifyRouteChanged()
        val first = hub.teleport?.id
        hub.notifyRouteChanged()

        assertNotNull(first)
        assertEquals(first!! + 1, hub.teleport?.id)
    }

    @Test
    fun `reset clears every event so a re-enabled pet cannot replay them`() {
        hub.emitTap(Offset(4f, 4f))
        hub.emitSwipe(Offset(5f, 5f))
        hub.notifyRouteChanged()

        hub.reset()

        assertNull(hub.tap)
        assertNull(hub.swipe)
        assertNull(hub.teleport)
        // id 仍从 1 开始：清空后重新计数不影响「新事件」语义
        hub.emitTap(Offset(7f, 7f))
        assertEquals(1, hub.tap?.id)
        assertTrue(hub.tap!!.atMillis > 0L)
    }
}
