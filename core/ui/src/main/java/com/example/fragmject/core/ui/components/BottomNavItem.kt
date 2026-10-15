package com.example.fragmject.core.ui.components

import androidx.compose.ui.graphics.Color
import com.example.fragmject.core.designsystem.AppColors

data class BottomNavItem(
    val label: String,
    val resId: Int,
    val selectedColor: Color = AppColors.orange,
    val unselectedColor: Color = AppColors.theme
)