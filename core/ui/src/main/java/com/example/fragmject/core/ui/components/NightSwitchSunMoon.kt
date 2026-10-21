package com.example.fragmject.core.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SunAndMoon(
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    canvasRadius: Dp,
    infiniteTransition: InfiniteTransition,
) {
    var initProgress by remember { mutableFloatStateOf(0f) }
    if (progress <= 0f) {
        initProgress = 0f
    } else if (progress >= 1f) {
        initProgress = 1f
    }
    val starRadius = canvasRadius * 0.5f
    val starDiameter = starRadius * 2f
    val moveDistance = canvasWidth - (canvasHeight - starDiameter) - starDiameter
    Box(
        modifier = Modifier.size(starDiameter),
    ) {
        if (progress >= initProgress) {
            Sun(
                progress,
                false,
                canvasHeight,
                canvasRadius,
                starRadius,
                moveDistance,
                infiniteTransition
            )
        }
        Moon(
            progress,
            progress < initProgress,
            canvasHeight,
            canvasRadius,
            starRadius,
            moveDistance,
            infiniteTransition,
        )
        if (progress < initProgress) {
            Sun(
                progress,
                true,
                canvasHeight,
                canvasRadius,
                starRadius,
                moveDistance,
                infiniteTransition
            )
        }
    }
}

/**
 * Sun
 * with 3 layer, from bottom to top:
 * 1: top shadow
 * 2: sun
 * 3: bottom shadow
 */
@Composable
fun Sun(
    progress: Float = 0f,
    reversal: Boolean = false,
    canvasHeight: Dp,
    canvasRadius: Dp,
    starRadius: Dp,
    moveDistance: Dp,
    infiniteTransition: InfiniteTransition,
) {
    val offset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "infiniteTransition",
    )
    val animationOffsetSun by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "animationOffsetSun",
    )
    val starDiameter = starRadius * 2f
    // for sun/moon change, this is Moon move animation
    val progressX = sunProgressX(progress, reversal, starRadius)
    Canvas(
        modifier = Modifier
            .offset(
                x = (canvasHeight - starDiameter) / 2f + moveDistance * progress,
                y = (canvasHeight - starDiameter) / 2f,
            )
            .graphicsLayer(alpha = 0.99f)
            .clip(RoundedCornerShape(canvasRadius))
            .clipToBounds()
            .width(starDiameter)
            .height(starDiameter),
    ) {
        drawSunTopShadow(progressX.toPx(), starRadius.toPx(), offset)
        drawSunBody(progress, animationOffsetSun, progressX.toPx(), starRadius.toPx(), offset)
        drawSunBottomShadow(progressX.toPx(), starRadius.toPx(), offset)
    }
}

/** 日月切换时的横向位移：两端固定，中间段随进度线性推进。 */
private fun sunProgressX(progress: Float, reversal: Boolean, starRadius: Dp): Dp = when {
    !reversal -> 0.dp
    progress <= PER_DISTANCE -> 0.dp
    progress >= (1 - PER_DISTANCE) -> starRadius * 2.5f
    else -> -starRadius * 2f * (progress - PER_DISTANCE) * (1 / (1 - PER_DISTANCE * 2))
}

/**
 * 日月圆盘的中心点。[xSign] / [ySign] 决定高光偏移方向，
 * 抖动项让圆盘每帧轻微漂移，模拟天体呼吸感。
 */
private fun DrawScope.orbCenter(
    starRadiusPx: Float,
    offset: Float,
    progressXPx: Float,
    xSign: Float,
    ySign: Float,
): Offset {
    val jitter = starRadiusPx * 0.005f * offset
    return Offset(
        x = size.width / 2f + xSign * starRadiusPx * 0.05f + jitter + progressXPx,
        y = size.height / 2f + ySign * starRadiusPx * 0.1f + jitter,
    )
}

private fun DrawScope.drawSunTopShadow(progressXPx: Float, starRadiusPx: Float, offset: Float) {
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)
        drawCircle(
            color = sunTopShadowColor,
            radius = starRadiusPx + starRadiusPx * 0.1f,
            center = Offset(size.width / 2f + progressXPx, size.height / 2f),
        )
        drawCircle(
            color = Color.Transparent,
            radius = starRadiusPx * 1.05f,
            center = orbCenter(starRadiusPx, offset, progressXPx, 1f, 1f),
            blendMode = BlendMode.Clear,
        )
        restoreToCount(checkPoint)
    }
}

private fun DrawScope.drawSunBody(
    progress: Float,
    animationOffsetSun: Float,
    progressXPx: Float,
    starRadiusPx: Float,
    offset: Float,
) {
    val colorProgress = if (progress == 0f) animationOffsetSun else progress
    drawCircle(
        color = offsetColor(sunColor, sunColorDeep, colorProgress),
        radius = starRadiusPx * 1.05f,
        center = orbCenter(starRadiusPx, offset, progressXPx, 1f, 1f),
    )
}

private fun DrawScope.drawSunBottomShadow(progressXPx: Float, starRadiusPx: Float, offset: Float) {
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)
        drawCircle(
            color = sunBottomShadowColor,
            radius = starRadiusPx + starRadiusPx * 0.1f,
            center = Offset(size.width / 2f + progressXPx, size.height / 2f),
        )
        drawCircle(
            color = Color.Transparent,
            radius = starRadiusPx,
            center = orbCenter(starRadiusPx, offset, progressXPx, -1f, -1f),
            blendMode = BlendMode.SrcIn,
        )
        restoreToCount(checkPoint)
    }
}

/**
 * Moon
 * with 4 layer, from bottom to top
 * 1: top shadow
 * 2: moon
 * 3: moon crater
 * 4: bottom shadow
 */
@Composable
fun Moon(
    progress: Float = 1f,
    reversal: Boolean = false,
    canvasHeight: Dp,
    canvasRadius: Dp,
    starRadius: Dp,
    moveDistance: Dp,
    infiniteTransition: InfiniteTransition,
) {
    val offset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offset",
    )

    val offsetMoonDown by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "offsetMoonDown",
    )
    val starDiameter = starRadius * 2f
    // for sun/moon change, this is Moon move animation
    val progressX = moonProgressX(progress, reversal, starRadius)

    Canvas(
        modifier = Modifier
            .offset(
                x = (canvasHeight - starDiameter) / 2f + moveDistance * progress,
                y = (canvasHeight - starDiameter) / 2f,
            )
            .graphicsLayer(alpha = 0.99f)
            .clip(RoundedCornerShape(canvasRadius))
            .clipToBounds()
            .width(starDiameter)
            .height(starDiameter)
    ) {
        drawMoonTopShadow(progressX.toPx(), starRadius.toPx(), offset)
        drawMoonBody(progressX.toPx(), starRadius.toPx(), offset)
        drawMoonCraters(offsetMoonDown, starRadius.toPx(), progressX.toPx(), offset)
        drawMoonBottomShadow(progressX.toPx(), starRadius.toPx(), offset)
    }
}

/** 月相位移与太阳相反：起止锚点互换。 */
private fun moonProgressX(progress: Float, reversal: Boolean, starRadius: Dp): Dp {
    if (reversal) return 0.dp
    return when {
        progress <= PER_DISTANCE -> starRadius * 2.5f
        progress >= (1 - PER_DISTANCE) -> 0.dp
        else -> starRadius * 2f - starRadius * 2f * (progress - PER_DISTANCE) * (1 / (1 - PER_DISTANCE * 2))
    }
}

private fun DrawScope.drawMoonTopShadow(progressXPx: Float, starRadiusPx: Float, offset: Float) {
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)
        drawCircle(
            color = moonTopShadowColor,
            radius = starRadiusPx + starRadiusPx * 0.1f,
            center = Offset(size.width / 2f + progressXPx, size.height / 2f),
        )
        drawCircle(
            color = Color.Transparent,
            radius = starRadiusPx * 1.05f,
            center = orbCenter(starRadiusPx, offset, progressXPx, 1f, 1f),
            blendMode = BlendMode.Clear,
        )
        restoreToCount(checkPoint)
    }
}

private fun DrawScope.drawMoonBody(progressXPx: Float, starRadiusPx: Float, offset: Float) {
    drawCircle(
        color = moonColor,
        radius = starRadiusPx * 1.05f,
        center = orbCenter(starRadiusPx, offset, progressXPx, 1f, 1f),
    )
}

/** 陨石坑：先铺一层月亮底色，再用 [moonDownColor] 挖出坑洞（SrcIn 只保留重叠区）。 */
private fun DrawScope.drawMoonCraters(
    offsetMoonDown: Float,
    starRadiusPx: Float,
    progressXPx: Float,
    offset: Float,
) {
    val drift = size.width * offsetMoonDown
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)
        drawCircle(
            color = moonColor,
            radius = starRadiusPx * 1.05f,
            center = orbCenter(starRadiusPx, offset, progressXPx, 1f, 1f),
        )
        drawCraterPair(starRadiusPx / 3f, size.width / 2f - height / 4f + drift, size.height / 5f * 3f)
        drawCraterPair(starRadiusPx / 4f, size.width / 2f + height / 6f + drift, size.height / 4f * 1f)
        drawCraterPair(starRadiusPx / 4f, size.width / 2f + height / 8f + drift, size.height / 4f * 3f)
        drawCraterPair(starRadiusPx / 6f, height / 8f + drift, size.height / 5f * 1f)
        restoreToCount(checkPoint)
    }
}

/**
 * 每个陨石坑画两次：一次在 [x] 处，一次在左移一屏处，
 * 与月面滚动动画配合，形成无缝循环。
 */
private fun DrawScope.drawCraterPair(radiusPx: Float, x: Float, y: Float) {
    drawCircle(
        color = moonDownColor,
        radius = radiusPx,
        center = Offset(x - size.width, y),
        blendMode = BlendMode.SrcIn,
    )
    drawCircle(
        color = moonDownColor,
        radius = radiusPx,
        center = Offset(x, y),
        blendMode = BlendMode.SrcIn,
    )
}

private fun DrawScope.drawMoonBottomShadow(progressXPx: Float, starRadiusPx: Float, offset: Float) {
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)
        drawCircle(
            color = moonBottomShadowColor,
            radius = starRadiusPx + starRadiusPx * 0.1f,
            center = Offset(size.width / 2f + progressXPx, size.height / 2f),
        )
        drawCircle(
            color = Color.Transparent,
            radius = starRadiusPx,
            center = orbCenter(starRadiusPx, offset, progressXPx, -1f, -1f),
            blendMode = BlendMode.SrcIn,
        )
        restoreToCount(checkPoint)
    }
}

/**
 * get random NightStar info
 */
@Stable
fun getRandomStart(): NightStar {
    val star = NightStar()
    star.x.floatValue = getRandom(0f, 1f)
    star.y.floatValue = getRandom(0f, 1f)
    star.radius.floatValue = getRandom(0f, 1f)
    star.status.value = NightStarStatus.Start
    return star
}
