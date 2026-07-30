package com.example.fragmject.core.designsystem

import androidx.compose.ui.graphics.Color

data class NavigationItem(
    val label: String,
    val resId: Int,
    val selectedColor: Color = WanColors.orange,
    val unselectedColor: Color = WanColors.theme
)