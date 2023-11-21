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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val appDarkColorScheme = darkColorScheme(
    primary = Color(0xFF000000),
    secondary = Color(0XFFFF8B80),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    error = Color(0xFFCF6679),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black,
    outline = Color(0xFFF0F0F0),
    outlineVariant = Color(0xFFF0F0F0),
)

private val appLightColorScheme = lightColorScheme(
    primary = Color(0xFF272A36),
    secondary = Color(0XFFFF8B80),
    background = Color(0XFFF5F5F5),
    surface = Color(0XFFF5F5F5),
    error = Color(0xFFB00020),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White,
    outline = Color(0xFFF0F0F0),
    outlineVariant = Color(0xFFF0F0F0),
)

@Composable
fun WanTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
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
