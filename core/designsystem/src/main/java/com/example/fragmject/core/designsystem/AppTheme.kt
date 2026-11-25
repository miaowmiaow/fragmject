package com.example.fragmject.core.designsystem

import android.view.Window
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/**
 * 主题 Composable 包装器：无状态，深色模式由 [darkTheme] 参数注入。
 *
 * 状态来源（ThemeRepository.observeDarkTheme）由 app 层在调用时收集并传入，
 * 使 designsystem 不再依赖任何全局可变状态（原 core:theme 的 DarkThemeState）。
 */

@Composable
fun AppTheme(
    window: Window? = null,
    darkTheme: Boolean = false,
    /**
     * 应用内字体缩放乘数（1f = 不缩放）。
     *
     * 由 app 层把「正文字号」换算后传入（见 `core:domain` 的 `FontScale.bodyFontScale`）；
     * designsystem 不认识业务字号，只负责把它乘到 `LocalDensity.fontScale` 上，
     * 与系统字体缩放**叠加**而不是替换。
     */
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {

    // 配置（如横竖屏切换）会触发 enableEdgeToEdge 重置系统栏外观，
    // 因此把 configuration 纳入 key，确保旋转后仍重新应用正确的图标颜色。
    val configuration = LocalConfiguration.current

    LaunchedEffect(window, darkTheme, configuration) {
        window?.let {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val appColorScheme = if (darkTheme) {
        AppDarkColorScheme
    } else {
        AppLightColorScheme
    }

    // 只改 fontScale，density 原样保留：dp 尺寸（图标、间距、固定高度）不跟着变，
    // 只有 sp 文字缩放。这就是「字号设置」与「整体缩放」的区别，别把 density 一起改。
    val density = LocalDensity.current
    val scaledDensity = remember(density, fontScale) {
        Density(density = density.density, fontScale = density.fontScale * fontScale)
    }

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        // 密集布局（日历/表格）用它把倍率还原成「系统字体缩放」，见 LocalAppFontScale
        LocalAppFontScale provides fontScale,
    ) {
        MaterialTheme(
            colorScheme = appColorScheme,
            content = content
        )
    }
}