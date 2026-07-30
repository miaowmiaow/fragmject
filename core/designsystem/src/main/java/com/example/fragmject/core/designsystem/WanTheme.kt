package com.example.fragmject.core.designsystem

import android.view.Window
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.common.DarkThemeState

/**
 * 主题 Composable 包装器：将 DarkThemeState 绑定到 Material3 配色方案。
 */

@Composable
fun WanTheme(window: Window? = null, content: @Composable () -> Unit) {

    val darkTheme by DarkThemeState.isDark.collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(window, darkTheme) {
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