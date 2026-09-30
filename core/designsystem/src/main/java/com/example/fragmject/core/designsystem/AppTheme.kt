package com.example.fragmject.core.designsystem

import android.view.Window
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
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

    MaterialTheme(
        colorScheme = appColorScheme,
        content = content
    )
}