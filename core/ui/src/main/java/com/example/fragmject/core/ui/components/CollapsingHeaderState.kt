package com.example.fragmject.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Stable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp

/**
 * 折叠头部状态：封装「头部高度动画 + 嵌套滚动连接」，
 * 供需要折叠头部的页面（MyCoinScreen / UserScreen 等）复用。
 */
@Stable
class CollapsingHeaderState(
    val titleBarSize: Dp,
    val targetHeight: Dp,
    val titleBarSizePx: Float,
    val targetHeightPx: Float,
    val targetPercent: Animatable<Float, AnimationVector1D>,
    val nestedScrollConnection: NestedScrollConnection,
)
