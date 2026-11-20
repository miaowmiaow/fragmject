package com.example.fragmject.core.pet

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 到点行为选择的守卫测试。
 *
 * 这些概率原本埋在 [FishSwimRuntime.onArrived] 里，只有跑真机才知道某个行为是否还在出现。
 * 抽出纯函数后，把「阈值边界」和「优先级」锁死：
 * - `chance = 0` 必须永不触发（改调参时最常见的回归是把它调成 0 却发现还在跳）；
 * - `chance = 1` 必须必定触发（`nextFloat()` 值域是 `[0,1)`，不会等于 1）；
 * - 多项同时命中时按 `LEAP > OBSERVE > CURIOUS > SPIN` 取优先级。
 */
class ArrivalPlannerTest {

    private val chances = ArrivalChances(leap = 0.2f, observe = 0.35f, curious = 0.25f, spin = 0.1f)

    private fun choose(
        leap: Float = 1f,
        observe: Float = 1f,
        curious: Float = 1f,
        spin: Float = 1f,
        chances: ArrivalChances = this.chances,
    ) = ArrivalPlanner.choose(leap, observe, curious, spin, chances)

    @Test
    fun `概率全为零时永远巡游`() {
        val zero = ArrivalChances(leap = 0f, observe = 0f, curious = 0f, spin = 0f)
        // nextFloat() 取不到 1，所以 0.999 是最坏情况
        assertEquals(ArrivalAction.CRUISE, choose(0.999f, 0.999f, 0.999f, 0.999f, zero))
    }

    @Test
    fun `落在阈值内即命中对应行为`() {
        assertEquals(ArrivalAction.LEAP, choose(leap = 0.19f))
        assertEquals(ArrivalAction.OBSERVE, choose(leap = 0.5f, observe = 0.34f))
        assertEquals(ArrivalAction.CURIOUS, choose(leap = 0.5f, observe = 0.5f, curious = 0.24f))
        assertEquals(ArrivalAction.SPIN, choose(0.5f, 0.5f, 0.5f, 0.09f))
        assertEquals(ArrivalAction.CRUISE, choose(0.5f, 0.5f, 0.5f, 0.5f))
    }

    @Test
    fun `等于阈值不命中（比较是严格小于）`() {
        // 保证 chance = 0 时"roll = 0"也不会误触发
        assertEquals(ArrivalAction.CRUISE, choose(0.2f, 0.35f, 0.25f, 0.1f))
    }

    @Test
    fun `概率为一必定命中`() {
        val always = ArrivalChances(leap = 1f, observe = 1f, curious = 1f, spin = 1f)
        assertEquals(ArrivalAction.LEAP, choose(0f, 0f, 0f, 0f, always))
    }

    @Test
    fun `多项命中时按跳跃优先`() {
        // 全部 roll 都很小：四个条件都成立，必须是 LEAP 先赢
        assertEquals(ArrivalAction.LEAP, choose(0.01f, 0.01f, 0.01f, 0.01f))
    }

    @Test
    fun `跳跃未命中后按观察优先`() {
        assertEquals(ArrivalAction.OBSERVE, choose(leap = 0.9f, observe = 0.01f, curious = 0.01f, spin = 0.01f))
    }
}
