package com.example.fragmject.core.domain.pet

/**
 * 漂浮宠物的用户配置（全局设置，不区分账号，与深色模式同口径）。
 *
 * 三个比例都是「相对基准值」，便于设置页用滑杆表达：
 * - [opacity] 不透明度，`0.2~1.0`
 * - [sizeScale] 头半径缩放，`0.6~1.8`
 * - [speedScale] 游动速度缩放，`0.5~2.0`
 *
 * [sanitized] 负责夹紧：KV 里可能是旧版本残留、被手工改过或是空串，
 * 不夹紧会出现「鱼完全透明」「鱼大得挡住整屏」这类无法自助恢复的状态。
 */
data class PetSettings(
    val enabled: Boolean = false,
    val opacity: Float = DEFAULT_OPACITY,
    val sizeScale: Float = DEFAULT_SIZE_SCALE,
    val speedScale: Float = DEFAULT_SPEED_SCALE,
) {

    fun sanitized(): PetSettings = PetSettings(
        enabled = enabled,
        opacity = opacity.coerceIn(MIN_OPACITY, MAX_OPACITY),
        sizeScale = sizeScale.coerceIn(MIN_SIZE_SCALE, MAX_SIZE_SCALE),
        speedScale = speedScale.coerceIn(MIN_SPEED_SCALE, MAX_SPEED_SCALE),
    )

    companion object {
        const val MIN_OPACITY = 0.2f
        const val MAX_OPACITY = 1f
        const val DEFAULT_OPACITY = 1f

        const val MIN_SIZE_SCALE = 0.6f
        const val MAX_SIZE_SCALE = 1.8f

        /** 初始尺寸取可调区间的最小值：默认只露出一条小鱼，不打扰阅读。 */
        const val DEFAULT_SIZE_SCALE = 0.6f

        const val MIN_SPEED_SCALE = 0.5f
        const val MAX_SPEED_SCALE = 2f

        /** 初始速度偏快一档：小尺寸下如果游得慢会显得"卡住不动"。 */
        const val DEFAULT_SPEED_SCALE = 1.5f

        /**
         * 首次启动 / KV 缺键时的取值。
         *
         * **默认不显示**：常驻宠物是装饰，不该在用户没主动开启时出现在每一页上；
         * 入口在 `设置 → 漂浮宠物`。
         */
        val Default = PetSettings()
    }
}
