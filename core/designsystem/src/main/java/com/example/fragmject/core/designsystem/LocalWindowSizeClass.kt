package com.example.fragmject.core.designsystem

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize

/**
 * 将 [WindowSizeClass] 注入 Compose 树，供所有子组件按需读取。
 *
 * 在 Activity.setContent {} 中通过 [rememberWindowSizeClass] 计算并设置。
 */
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("LocalWindowSizeClass not provided — wrap with rememberWindowSizeClass() in setContent {}")
}

/**
 * 便捷扩展属性，可直接用于 when/if 判断。
 */
val WindowSizeClass.isCompact get() = widthSizeClass == WindowWidthSizeClass.Compact
val WindowSizeClass.isMedium get() = widthSizeClass == WindowWidthSizeClass.Medium
val WindowSizeClass.isExpanded get() = widthSizeClass == WindowWidthSizeClass.Expanded

/**
 * 在 setContent {} 最外层调用，计算当前窗口尺寸等级并注入 [LocalWindowSizeClass]。
 *
 * 用法：
 * ```kotlin
 * setContent {
 *     AppTheme(window) {
 *         rememberWindowSizeClass {
 *             AppNavGraph()
 *         }
 *     }
 * }
 * ```
 */
// rememberXxx 是 Compose 的工厂函数命名约定，不适用「Composable 返回 Unit 需大写开头」规则。
@Suppress("ComposableNaming")
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(content: @Composable () -> Unit) {
    // 用 LocalWindowInfo.containerSize 而非 LocalConfiguration.screenWidthDp：
    // 分屏/自由窗口下 Configuration 可能滞后，containerSize 反映本窗口当前可用尺寸。
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val windowSizeClass = remember(containerSize, density) {
        with(density) {
            WindowSizeClass.calculateFromSize(
                DpSize(containerSize.width.toDp(), containerSize.height.toDp())
            )
        }
    }
    CompositionLocalProvider(
        LocalWindowSizeClass provides windowSizeClass
    ) {
        content()
    }
}