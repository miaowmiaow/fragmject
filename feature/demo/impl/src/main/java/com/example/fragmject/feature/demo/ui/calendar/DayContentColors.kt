package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.fragmject.core.designsystem.AppColors

/**
 * 公历日数字的颜色：非当前月统一弱化，选中且非节日时用反色，其余保持正文色。
 */
internal fun dayNumberColor(
    colors: ColorScheme,
    isCurrMonth: Boolean,
    isSelected: Boolean,
    hasFestival: Boolean,
): Color = when {
    !isCurrMonth -> colors.onTertiary
    isSelected && !hasFestival -> colors.secondaryContainer
    else -> colors.onPrimary
}

/**
 * 农历/节日小字的颜色：选中态优先，其次是「节日在当月 / 跨月」两种强调色。
 */
internal fun lunarLabelColor(
    colors: ColorScheme,
    isCurrMonth: Boolean,
    isSelected: Boolean,
    hasFestival: Boolean,
    isMonthFillMode: Boolean,
): Color = when {
    isCurrMonth && isSelected && hasFestival -> colors.onSecondaryContainer
    isCurrMonth && isSelected -> colors.secondaryContainer
    isCurrMonth && hasFestival && !isMonthFillMode -> colors.onSecondaryContainer
    !isCurrMonth && hasFestival && !isMonthFillMode -> AppColors.alphaOrange
    else -> colors.onTertiary
}
