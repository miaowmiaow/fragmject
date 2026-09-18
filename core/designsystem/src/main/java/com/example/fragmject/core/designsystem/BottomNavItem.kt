package com.example.fragmject.core.designsystem

import androidx.compose.ui.graphics.Color

data class BottomNavItem(
    val label: String,
    val resId: Int,
    val selectedColor: Color = AppColors.orange,
    val unselectedColor: Color = AppColors.theme
)