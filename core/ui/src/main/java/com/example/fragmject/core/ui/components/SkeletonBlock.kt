package com.example.fragmject.core.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** 骨架基础色：react-loading-skeleton 官方默认配色。 */
internal val SkeletonBaseDark = Color(0xFF202020)
internal val SkeletonBaseLight = Color(0xFFEBEBEB)

@Composable
internal fun isDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * 骨架占位块：使用业界标准灰阶基础色，明暗模式自动切换，视觉柔和。
 */
@Composable
internal fun SkeletonBlock(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    val baseColor = if (isDarkTheme()) SkeletonBaseDark else SkeletonBaseLight
    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor)
            .then(shimmerModifier())
    )
}

/** shimmer 高光色：react-loading-skeleton 官方默认配色。 */
internal val ShimmerHighlightDark = Color(0xFF444444)
internal val ShimmerHighlightLight = Color(0xFFF5F5F5)

/**
 * 完全复刻 react-loading-skeleton 的扫光动画：
 * 一块与骨架等宽、`base → highlight → base` 的渐变带，
 * 从 -100% 平移到 +100%（ease-in-out，周期 1.5s），
 * 高光随渐变带平滑滑过，而非孤立窄亮带。
 */
@Composable
internal fun shimmerModifier(): Modifier {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val baseColor = if (isDarkTheme()) SkeletonBaseDark else SkeletonBaseLight
    val highlightColor = if (isDarkTheme()) ShimmerHighlightDark else ShimmerHighlightLight
    return Modifier.drawWithContent {
        drawContent()
        val shift = progress * size.width
        // Brush 默认 Clamp：渐变带移出块时块显示 base 色，滑过时显示 base→highlight→base，
        // 精确复刻 react-loading-skeleton 的 ::after 满宽渐变带动画。
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(shift, 0f),
                end = Offset(shift + size.width, 0f)
            )
        )
    }
}
