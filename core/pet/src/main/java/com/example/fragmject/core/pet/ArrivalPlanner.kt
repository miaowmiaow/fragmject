package com.example.fragmject.core.pet

/** 鱼到达目标点后可以做的事。 */
internal enum class ArrivalAction { LEAP, OBSERVE, CURIOUS, SPIN, CRUISE }

/**
 * 到点后的默认概率（克制版：合计 90% 里绝大部分仍然是形态相近的观察/好奇，跳跃只占 20%）。
 *
 * 放在这里而不是运行时里：运行时已经 400+ 行（规则 28 上限 500），
 * 概率是"调参对象"，跟判定逻辑放在一起更好找。
 */
internal val DEFAULT_ARRIVAL_CHANCES = ArrivalChances(
    leap = 0.20f,
    observe = 0.35f,
    curious = 0.25f,
    spin = 0.10f,
)

/** 各行为的触发概率（0..1）。集中成一个对象，避免纯函数参数表超限。 */
internal class ArrivalChances(
    val leap: Float,
    val observe: Float,
    val curious: Float,
    val spin: Float,
)

/**
 * 到点行为的**纯函数**版本。
 *
 * 从 [FishSwimRuntime.onArrived] 抽出来的原因：概率与优先级是"调参时最容易静默回归"的部分
 * （改一个 chance 就可能让某个行为再也不出现），而它们原本埋在 Compose 状态里无法单测。
 * 抽出来以后运行时只负责取随机数与执行，判定本身完全可测。
 *
 * 优先级固定为 `LEAP > OBSERVE > CURIOUS > SPIN > CRUISE`，每一项都是 `<` 比较：
 * `roll == chance` 视为不命中，保证 `chance = 0` 时永不触发、`chance = 1` 时必定触发
 * （`Random.nextFloat()` 的值域是 `[0, 1)`，不会取到 1，所以 1 等价于必中）。
 */
internal object ArrivalPlanner {

    /** 取随机数的便捷重载：运行时直接调它，不关心默认概率具体是多少。 */
    fun choose(random: kotlin.random.Random = kotlin.random.Random): ArrivalAction = choose(
        leapRoll = random.nextFloat(),
        observeRoll = random.nextFloat(),
        curiousRoll = random.nextFloat(),
        spinRoll = random.nextFloat(),
        chances = DEFAULT_ARRIVAL_CHANCES,
    )

    fun choose(
        leapRoll: Float,
        observeRoll: Float,
        curiousRoll: Float,
        spinRoll: Float,
        chances: ArrivalChances,
    ): ArrivalAction = when {
        leapRoll < chances.leap -> ArrivalAction.LEAP
        observeRoll < chances.observe -> ArrivalAction.OBSERVE
        curiousRoll < chances.curious -> ArrivalAction.CURIOUS
        spinRoll < chances.spin -> ArrivalAction.SPIN
        else -> ArrivalAction.CRUISE
    }
}
