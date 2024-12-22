package com.example.fragment.project.data

import androidx.compose.ui.graphics.Color
import com.example.fragment.project.WanTheme

data class NavigationItem(
    val label: String,
    val resId: Int,
    val selectedColor: Color = WanTheme.orange,
    val unselectedColor: Color = WanTheme.theme
)