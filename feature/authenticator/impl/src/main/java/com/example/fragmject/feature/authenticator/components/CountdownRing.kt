package com.example.fragmject.feature.authenticator.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 倒计时圆环：包在账户头像外圈，随时间片消耗逆时针减少（实际为顺时针递减 sweep）。
 *
 * 剩余时间低于 [warningThreshold] 时变红，提示「这串码快过期了，复制要抓紧」。
 * 进度与颜色都做了动画，避免每 500ms 一跳的顿挫感。
 */
@Composable
fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier,
    sizeDp: Int = 48,
    strokeWidth: Float = 4f,
    warningThreshold: Float = 0.17f,
) {
    val isWarning = progress <= warningThreshold
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progress",
    )
    val color by animateColorAsState(
        // 项目暗色方案未定义 primary（会用 M3 默认紫），强调色统一取 AppColors.orange
        targetValue = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(300),
        label = "color",
    )
    val bgColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Box(modifier = modifier.size(sizeDp.dp)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = (sizeDp - strokeWidth) / 2f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            drawCircle(color = bgColor, radius = radius, center = center, style = stroke)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = stroke,
            )
        }
    }
}
