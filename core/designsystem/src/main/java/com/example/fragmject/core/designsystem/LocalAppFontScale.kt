package com.example.fragmject.core.designsystem

import androidx.compose.runtime.compositionLocalOf

/**
 * 应用内字体缩放倍率（1f = 不叠加），由 [AppTheme] 提供。
 *
 * 用途：少数**密集布局**（日历网格、表格）不适合叠加应用内倍率 —— 它们的单元格是
 * 固定比例（如 `aspectRatio(1f)` + 圆形裁剪），字号再放大就会被裁。
 * 这些组件读取本值，把 `LocalDensity.fontScale` 还原成「系统字体缩放」：
 *
 * ```kotlin
 * val density = LocalDensity.current
 * val appScale = LocalAppFontScale.current
 * val systemOnly = Density(density.density, density.fontScale / appScale)
 * ```
 *
 * 这样**系统字体放大仍然生效**（无障碍设置被尊重），只是不叠加应用内的额外放大。
 * 不要用它去「关掉」普通页面的字号跟随 —— 那只会在用户放大字号时显得设置失灵。
 */
val LocalAppFontScale = compositionLocalOf { 1f }
