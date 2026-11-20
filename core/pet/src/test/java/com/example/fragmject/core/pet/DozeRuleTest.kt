package com.example.fragmject.core.pet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 打盹判定的守卫测试。
 *
 * 第二条断言（屏外不入睡）对应一个真实问题：软边界允许鱼游出屏幕后，
 * 如果它正好在屏外到达打盹时间，就会停在屏幕外睡 30~60 秒，看起来像宠物消失了。
 * 这个场景在真机上偶发、极难复现，只能靠纯函数用例守住。
 */
class DozeRuleTest {

    @Test
    fun `空闲时间不足时不睡`() {
        assertFalse(DozeRule.shouldDoze(idleSeconds = 10f, dozeAfterSeconds = 30f, insideScreen = true))
    }

    @Test
    fun `空闲足够且在屏内时入睡`() {
        assertTrue(DozeRule.shouldDoze(idleSeconds = 31f, dozeAfterSeconds = 30f, insideScreen = true))
    }

    @Test
    fun `在屏幕外一律不睡`() {
        assertFalse(DozeRule.shouldDoze(idleSeconds = 999f, dozeAfterSeconds = 30f, insideScreen = false))
    }

    @Test
    fun `刚好等于阈值不睡（比较是严格大于）`() {
        assertFalse(DozeRule.shouldDoze(idleSeconds = 30f, dozeAfterSeconds = 30f, insideScreen = true))
    }
}
