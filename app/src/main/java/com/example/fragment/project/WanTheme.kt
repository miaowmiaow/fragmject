/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.fragment.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object WanTheme {
    internal val theme = Color(0xFF272A36)
    internal val blue = Color(0xFF508CEE)
    internal val green = Color(0xFF33CC99)
    internal val orange = Color(0xFFFF8B80)
    internal val pink = Color(0xFFFF6C6C)
    internal val red = Color(0xFFFF0000)
    internal val yellow = Color(0xFFFFB636)
    internal val alphaOrange = Color(0xB0FF8B80)
    internal val alphaGray = Color(0x39F5F5F5)
}

val appDarkColorScheme = darkColorScheme(
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

val appLightColorScheme = lightColorScheme(
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

@Composable
fun WanTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val appColorScheme = if (darkTheme) {
        appDarkColorScheme
    } else {
        appLightColorScheme
    }

    MaterialTheme(
        colorScheme = appColorScheme,
        content = content
    )
}
