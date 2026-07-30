package com.example.fragmject.core.designsystem

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

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
 *     WanTheme(window) {
 *         rememberWindowSizeClass {
 *             WanNavGraph()
 *         }
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val windowSizeClass = remember(config.screenWidthDp, config.screenHeightDp) {
        WindowSizeClass.calculateFromSize(
            DpSize(config.screenWidthDp.dp, config.screenHeightDp.dp)
        )
    }
    CompositionLocalProvider(
        LocalWindowSizeClass provides windowSizeClass
    ) {
        content()
    }
}