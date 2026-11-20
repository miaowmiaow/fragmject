package com.example.fragmject.core.domain.pet

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [PetSettings.sanitized] 的守卫测试。
 *
 * 这一层夹紧的意义：KV 里可能是旧版本残留、被手工改过或空串。
 * 没有夹紧时最坏的表现是「鱼完全透明看不出在哪」「鱼大到挡住整屏」，
 * 用户在设置页也无法自救（滑杆范围本身就限制了这个区间）。
 */
class PetSettingsTest {

    private val epsilon = 0.0001f

    @Test
    fun `非法透明度被夹到可调区间`() {
        assertEquals(PetSettings.MIN_OPACITY, PetSettings(opacity = -1f).sanitized().opacity, epsilon)
        assertEquals(PetSettings.MIN_OPACITY, PetSettings(opacity = 0f).sanitized().opacity, epsilon)
        assertEquals(PetSettings.MAX_OPACITY, PetSettings(opacity = 3f).sanitized().opacity, epsilon)
    }

    @Test
    fun `非法尺寸被夹到可调区间`() {
        assertEquals(PetSettings.MIN_SIZE_SCALE, PetSettings(sizeScale = 0.1f).sanitized().sizeScale, epsilon)
        assertEquals(PetSettings.MAX_SIZE_SCALE, PetSettings(sizeScale = 9f).sanitized().sizeScale, epsilon)
    }

    @Test
    fun `非法速度被夹到可调区间`() {
        assertEquals(PetSettings.MIN_SPEED_SCALE, PetSettings(speedScale = 0f).sanitized().speedScale, epsilon)
        assertEquals(PetSettings.MAX_SPEED_SCALE, PetSettings(speedScale = 10f).sanitized().speedScale, epsilon)
    }

    @Test
    fun `合法值原样保留`() {
        val settings = PetSettings(enabled = false, opacity = 0.6f, sizeScale = 1.4f, speedScale = 1.8f)
        assertEquals(settings, settings.sanitized())
    }

    @Test
    fun `默认值合法且不被改写`() {
        assertEquals(PetSettings.Default, PetSettings.Default.sanitized())
        // 默认不显示：常驻装饰不该在用户没开启时出现在每一页上
        assertEquals(false, PetSettings.Default.enabled)
        assertEquals(1f, PetSettings.Default.opacity, epsilon)
        assertEquals(0.6f, PetSettings.Default.sizeScale, epsilon)
        assertEquals(1.5f, PetSettings.Default.speedScale, epsilon)
    }
}
