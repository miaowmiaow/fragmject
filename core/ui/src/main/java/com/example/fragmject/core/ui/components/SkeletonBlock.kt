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
import androidx.compose.runtime.remember
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
 * 全局共享的 shimmer 进度（返回值在 **draw 阶段**读取）。
 *
 * 若每个骨架块各自 `rememberInfiniteTransition`，6 张卡 × 5 块 = 30 个独立动画，
 * 每帧各自新建 Brush/Shader（约 1800 次/秒的分配）。这里只创建一个 InfiniteTransition，
 * 并由调用方把进度以 lambda 形式向下传递：进度在 draw 阶段读取，
 * 因此动画推进**不会触发任何重组**，只触发绘制失效。
 */
@Composable
internal fun rememberShimmerProgress(): () -> Float {
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
    return { progress }
}

/**
 * 骨架占位块：使用业界标准灰阶基础色，明暗模式自动切换，视觉柔和。
 *
 * [shimmerProgress] 由 [rememberShimmerProgress] 提供并在 draw 阶段读取，
 * 避免每个块独立持有一个无限动画。
 */
@Composable
internal fun SkeletonBlock(
    modifier: Modifier,
    shimmerProgress: () -> Float,
    /** shimmer 相位偏移（0f..2f，与一个动画周期等长），用于错开各块的扫光时刻。 */
    shimmerPhase: Float = 0f,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    val baseColor = if (isDarkTheme()) SkeletonBaseDark else SkeletonBaseLight
    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor)
            .then(shimmerModifier(shimmerProgress, shimmerPhase))
    )
}

/**
 * 由索引生成稳定的错落相位（0f..2f）。
 *
 * 用黄金比例小数部分做等分布：相邻索引的相位差约为 0.618 个周期，
 * 既不重复也不聚集，视觉上呈自然的错落扫光，且对同一索引恒定（不会随重组抖动）。
 */
internal fun shimmerPhaseOf(index: Int): Float =
    ((index * 0.6180339887f) % 1f) * 2f

/** shimmer 高光色：react-loading-skeleton 官方默认配色。 */
internal val ShimmerHighlightDark = Color(0xFF444444)
internal val ShimmerHighlightLight = Color(0xFFF5F5F5)

/**
 * 完全复刻 react-loading-skeleton 的扫光动画：
 * 一块与骨架等宽、`base → highlight → base` 的渐变带，
 * 从 -100% 平移到 +100%（ease-in-out，周期 1.5s），
 * 高光随渐变带平滑滑过，而非孤立窄亮带。
 *
 * [progress] 以 lambda 传入并在 draw 阶段读取：滑块位移不触发重组，
 * 只让 `drawWithContent` 重绘；颜色表按主题缓存，避免每帧新建 List。
 *
 * [phase] 让各块在同一周期内错开：进度按 `(base + 1 + phase) % 2 - 1` 折叠回 [-1,1)，
 * 折叠发生在光带完全移出块外（两侧均为 base 色）的位置，因此回绕不可见。
 */
@Composable
internal fun shimmerModifier(progress: () -> Float, phase: Float = 0f): Modifier {
    val baseColor = if (isDarkTheme()) SkeletonBaseDark else SkeletonBaseLight
    val highlightColor = if (isDarkTheme()) ShimmerHighlightDark else ShimmerHighlightLight
    val colors = remember(baseColor, highlightColor) {
        listOf(baseColor, highlightColor, baseColor)
    }
    return Modifier.drawWithContent {
        drawContent()
        val shifted = (progress() + 1f + phase) % 2f - 1f
        val shift = shifted * size.width
        // Brush 默认 Clamp：渐变带移出块时块显示 base 色，滑过时显示 base→highlight→base，
        // 精确复刻 react-loading-skeleton 的 ::after 满宽渐变带动画。
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(shift, 0f),
                end = Offset(shift + size.width, 0f)
            )
        )
    }
}
