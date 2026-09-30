package com.example.fragmject.feature.article.ui.web

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.ui.R

/**
 * 网页加载进度条：弱化轨道 + 主体进度 + 头部渐变光晕 + 高亮点。
 * 从原 WebScreen 抽出，供独立复用。
 */
@Composable
fun WebProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val progressColor = colorResource(R.color.theme_orange)
    // 进度值平滑插值，避免 onProgressChanged 的离散跳变；完成时稍快收尾
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (progress >= 1f) {
            tween(durationMillis = 180)
        } else {
            spring(stiffness = Spring.StiffnessLow)
        },
        label = "webProgress"
    )
    AnimatedVisibility(
        visible = (animatedProgress > 0f && animatedProgress < 1f),
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(260))
    ) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(3.dp)
        ) {
            val trackHeight = size.height
            val width = size.width
            val cap = trackHeight / 2f
            // 弱化的轨道：极淡的同色调底
            drawLine(
                color = progressColor.copy(alpha = 0.12f),
                start = Offset(0f, trackHeight / 2f),
                end = Offset(width, trackHeight / 2f),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
            val progressWidth = (width * animatedProgress).coerceIn(0f, width)
            if (progressWidth > 0f) {
                // 主体进度
                drawLine(
                    color = progressColor,
                    start = Offset(0f, trackHeight / 2f),
                    end = Offset(progressWidth, trackHeight / 2f),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
                // 头部渐变拖尾光晕：从透明到主色，集中在头部约 24dp 区间
                val glowWidth = 24.dp.toPx().coerceAtMost(progressWidth)
                val glowStart = (progressWidth - glowWidth).coerceAtLeast(0f)
                val brush = Brush.horizontalGradient(
                    colors = listOf(
                        progressColor.copy(alpha = 0f),
                        progressColor.copy(alpha = 0.55f)
                    ),
                    startX = glowStart,
                    endX = progressWidth
                )
                drawRect(
                    brush = brush,
                    topLeft = Offset(glowStart, 0f),
                    size = Size(glowWidth, trackHeight)
                )
                // 头部高亮点：让"前进感"更明显
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = cap * 0.55f,
                    center = Offset(progressWidth - cap, trackHeight / 2f),
                    style = Stroke(width = 0f)
                )
            }
        }
    }
}
