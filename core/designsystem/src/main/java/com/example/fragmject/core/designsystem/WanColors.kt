package com.example.fragmject.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 全局主题颜色常量与 Material3 配色方案。
 *
 * 原置于 :app 的 [WanTheme] 中，现将颜色定义提升至 core:ui，
 * 使 feature 模块的共享 UI 组件可以直接引用，无需依赖 :app。
 *
 * 暗色模式切换逻辑仍保留在 :app 的 WanTheme Composable 中。
 */
object WanColors {
    val theme = Color(0xFF272A36)
    val blue = Color(0xFF508CEE)
    val green = Color(0xFF33CC99)
    val orange = Color(0xFFFF8B80)
    val pink = Color(0xFFFF6C6C)
    val red = Color(0xFFFF0000)
    val yellow = Color(0xFFFFB636)
    val alphaOrange = Color(0xB0FF8B80)
    val alphaGray = Color(0x39F5F5F5)
}

val AppDarkColorScheme = darkColorScheme(
    onPrimary = Color(0xFFCCCCCC),
    primaryContainer = Color(0xFF272A36),
    onPrimaryContainer = Color(0xFF999999),
    secondary = Color(0xFFFF8B80),
    onSecondary = Color(0xFF999999),
    secondaryContainer = Color(0xFF444444),
    onSecondaryContainer = Color(0xFFFF8B80),
    onTertiary = Color(0xFF666666),
    tertiaryContainer = Color(0xFF444444),
    onTertiaryContainer = Color(0xFF508CEE),
    background = Color(0xFF222222),
    onBackground = Color(0xFF333333),
    surface = Color(0xFF444444),
    onSurface = Color(0xFFFF8B80),
    surfaceVariant = Color(0xFF444444),
    onSurfaceVariant = Color(0xFF333333),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFF888888),
    error = Color(0xFFFF0000),
    onError = Color(0xFFFF0000),
    errorContainer = Color(0xFFFF0000),
    onErrorContainer = Color(0xFFFF0000),
    surfaceContainer = Color(0xFF444444),
    surfaceContainerHigh = Color(0xFF3D3D3D),
    surfaceContainerLow = Color(0xFF444444),
)

val AppLightColorScheme = lightColorScheme(
    onPrimary = Color(0xFF333333),
    primaryContainer = Color(0xFF272A36),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFFF8B80),
    onSecondary = Color(0xFF666666),
    secondaryContainer = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFFFF8B80),
    onTertiary = Color(0xFF999999),
    tertiaryContainer = Color(0xFFFFFFFF),
    onTertiaryContainer = Color(0xFF508CEE),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF666666),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFF8B80),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF272A36),
    outline = Color(0xFFF0F0F0),
    outlineVariant = Color(0xFFF0F0F0),
    error = Color(0xFFFF0000),
    onError = Color(0xFFFF0000),
    errorContainer = Color(0xFFFF0000),
    onErrorContainer = Color(0xFFFF0000),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFC5C5C5),
    surfaceContainerLow = Color(0xFFFFFFFF),
)
