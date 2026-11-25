package com.example.fragmject.core.domain.text

/**
 * 应用内界面字体缩放的取值域与换算。
 *
 * 单位选择：滑块以**正文字号（sp）**为单位、1sp 步进，而不是「1.15 倍」——
 * 用户对字号有直觉（14sp / 16sp），对倍率没有；换算成
 * `LocalDensity(fontScale = …)` 所需的乘数由 [bodyFontScale] 承担。
 *
 * 上限 20sp（约 1.43 倍）：已经用 Roborazzi 在 1.0x / 1.28x / 1.43x 三档出图验收过
 * `ArrowRightItem`、`TitleBar`、`FeedCard` 与日历日格（1.43x 下前两者与卡片均无硬裁，
 * 只有水平的省略号；日历不叠加应用倍率）。再往上调之前，请先给剩余页面补截图。
 */
object FontScale {

    /** 正文基准字号：界面上最常用的正文字号，也是滑块的 1.0 倍点。 */
    const val BASE_BODY_SP = 14f

    /** 下限：再小会明显难读，且与系统字体缩放的叠加会失去意义。 */
    const val MIN_BODY_SP = 13f

    /** 上限：20sp ≈ 1.43x，见类注释的验收依据。 */
    const val MAX_BODY_SP = 20f

    const val DEFAULT_BODY_SP = BASE_BODY_SP

    fun clampBodySp(value: Float): Float = value.coerceIn(MIN_BODY_SP, MAX_BODY_SP)

    /**
     * 字号 → `LocalDensity(fontScale = …)` 的乘数。
     *
     * 注意这是**乘在系统 fontScale 之上**的：写成 `density.fontScale * bodyFontScale(sp)`，
     * 而不是直接替换，否则用户把系统字体调大后进应用反而变小。
     */
    fun bodyFontScale(bodySp: Float): Float = clampBodySp(bodySp) / BASE_BODY_SP
}
