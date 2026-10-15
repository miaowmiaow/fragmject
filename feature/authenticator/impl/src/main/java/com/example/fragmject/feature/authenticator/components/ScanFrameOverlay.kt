package com.example.fragmject.feature.authenticator.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.fragmject.feature.authenticator.model.ScanState

/**
 * 取景遮罩：全屏半透明黑 + 中间圆角透明窗口 + 边框（参考 HugeRivers/Authenticator）。
 *
 * 识别到时窗口放大并变绿，给用户一个明确「扫到了」的反馈，
 * 否则相机画面里没有任何变化时用户不知道该不该继续对。
 */
@Composable
fun ScanFrameOverlay(
    state: ScanState,
    modifier: Modifier = Modifier,
) {
    val detected = state is ScanState.Detected || state is ScanState.Success
    val scale by animateFloatAsState(
        targetValue = if (detected) 1.15f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "zoom",
    )
    val borderColor = when (state) {
        is ScanState.Scanning -> MaterialTheme.colorScheme.secondaryContainer
        is ScanState.Detected -> MaterialTheme.colorScheme.onSecondaryContainer
        is ScanState.Success -> MaterialTheme.colorScheme.onSecondaryContainer
        is ScanState.Error -> MaterialTheme.colorScheme.errorContainer
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                val baseSize = size.minDimension * 0.62f
                val frameSize = baseSize * scale
                val frame = Rect(
                    Offset((size.width - frameSize) / 2f, (size.height - frameSize) / 2f),
                    Size(frameSize, frameSize),
                )
                drawIntoCanvas { canvas ->
                    val dimPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(150, 0, 0, 0)
                    }
                    canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, dimPaint)
                    val clearPaint = android.graphics.Paint().apply {
                        xfermode = android.graphics.PorterDuffXfermode(
                            android.graphics.PorterDuff.Mode.CLEAR,
                        )
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        frame.left, frame.top, frame.right, frame.bottom,
                        16.dp.toPx(), 16.dp.toPx(), clearPaint,
                    )
                }
                drawRoundRect(
                    color = borderColor,
                    topLeft = frame.topLeft,
                    size = frame.size,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = if (detected) 4.dp.toPx() else 2.dp.toPx()),
                )
            },
    )
}

/** 取景框内上下往复的扫描线（宽度与取景框一致）。 */
@Composable
fun ScanLineAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "progress",
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 与 ScanFrameOverlay 的 0.62f 保持一致
        val frameSize = minOf(maxWidth, maxHeight) * 0.62f
        Box(
            modifier = Modifier
                .width(frameSize)
                .height(2.dp)
                .offset { IntOffset(0, (frameSize * (progress - 0.5f)).roundToPx()) }
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(1.dp)),
        )
    }
}
