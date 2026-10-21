package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.runtime.Immutable

/** 周/月/月填充三种模式下的行高，单位 px。 */
@Immutable
internal data class WeekContentHeights(
    val weekModeHeight: Float,
    val monthModeHeight: Float,
    val monthFillModeHeight: Float,
)
