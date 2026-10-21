package com.example.fragmject.core.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random

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
    val progressX = if (reversal) {
        if (progress <= perDistance) {
            0.dp
        } else if (progress >= (1 - perDistance)) {
            starRadius * 2.5f
        } else {
            -starRadius * 2f * (progress - perDistance) * (1 / (1 - perDistance * 2))
        }
    } else {
        0.dp
    }
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
        // 1: top shadow
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            drawCircle(
                color = sunTopShadowColor,
                radius = starRadius.toPx() + starRadius.toPx() * 0.1f,
                center = Offset(size.width / 2f + progressX.toPx(), size.height / 2f),
            )
            drawCircle(
                color = Color.Transparent,
                radius = starRadius.toPx() * 1.05f,
                center = Offset(
                    size.width / 2f + starRadius.toPx() * 0.05f + starRadius.toPx() * 0.005f * offset + progressX.toPx(),
                    size.height / 2f + starRadius.toPx() * 0.1f + starRadius.toPx() * 0.005f * offset,
                ),
                blendMode = BlendMode.Clear,
            )
            restoreToCount(checkPoint)
        }
        // 2: sun
        drawCircle(
            color = offsetColor(
                sunColor, sunColorDeep, if (progress == 0f) {
                    animationOffsetSun
                } else {
                    progress
                }
            ),
            radius = starRadius.toPx() * 1.05f,
            center = Offset(
                size.width / 2f + starRadius.toPx() * 0.05f + starRadius.toPx() * 0.005f * offset + progressX.toPx(),
                size.height / 2f + starRadius.toPx() * 0.1f + starRadius.toPx() * 0.005f * offset,
            ),
        )
        // 3: bottom shadow
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            drawCircle(
                color = sunBottomShadowColor,
                radius = starRadius.toPx() + starRadius.toPx() * 0.1f,
                center = Offset(size.width / 2f + progressX.toPx(), size.height / 2f),
            )
            drawCircle(
                color = Color.Transparent,
                radius = starRadius.toPx(),
                center = Offset(
                    size.width / 2f - starRadius.toPx() * 0.05f + starRadius.toPx() * 0.005f * offset + progressX.toPx(),
                    size.height / 2f - starRadius.toPx() * 0.1f + starRadius.toPx() * 0.005f * offset,
                ),
                blendMode = BlendMode.SrcIn,
            )
            restoreToCount(checkPoint)
        }
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
    val progressX = if (reversal) {
        0.dp
    } else {
        if (progress <= perDistance) {
            starRadius * 2.5f
        } else if (progress >= (1 - perDistance)) {
            0.dp
        } else {
            starRadius * 2f - starRadius * 2f * (progress - perDistance) * (1 / (1 - perDistance * 2))
        }
    }

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
        // 1: top shadow
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            drawCircle(
                color = moonTopShadowColor,
                radius = starRadius.toPx() + starRadius.toPx() * 0.1f,
                center = Offset(size.width / 2f + progressX.toPx(), size.height / 2f),
            )
            drawCircle(
                color = Color.Transparent,
                radius = starRadius.toPx() * 1.05f,
                center = Offset(
                    size.width / 2f + starRadius.toPx() * 0.05f + starRadius.toPx() * 0.005f * offset + progressX.toPx(),
                    size.height / 2f + starRadius.toPx() * 0.1f + starRadius.toPx() * 0.005f * offset,
                ),
                blendMode = BlendMode.Clear,
            )
            restoreToCount(checkPoint)
        }

        // 2: moon
        drawCircle(
            color = moonColor,
            radius = starRadius.toPx() * 1.05f,
            center = Offset(
                size.width / 2f + starRadius.toPx() * 0.05f + starRadius.toPx() * 0.005f * offset + progressX.toPx(),
                size.height / 2f + starRadius.toPx() * 0.1f + starRadius.toPx() * 0.005f * offset,
            ),
        )

        // 3: moon crater
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            drawCircle(
                color = moonColor,
                radius = starRadius.toPx() * 1.05f,
                center = Offset(
                    size.width / 2f + starRadius.toPx() * 0.05f + starRadius.toPx() * 0.005f * offset + progressX.toPx(),
                    size.height / 2f + starRadius.toPx() * 0.1f + starRadius.toPx() * 0.005f * offset,
                ),
            )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 3f,
                center = Offset(
                    size.width / 2f - height / 4f + size.width * offsetMoonDown - size.width,
                    size.height / 5f * 3f,
                ),
                blendMode = BlendMode.SrcIn,

                )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 3f,
                center = Offset(
                    size.width / 2f - height / 4f + size.width * offsetMoonDown,
                    size.height / 5f * 3f,
                ),
                blendMode = BlendMode.SrcIn,
            )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 4f,
                center = Offset(
                    size.width / 2f + height / 6f + size.width * offsetMoonDown - size.width,
                    size.height / 4f * 1f,
                ),
                blendMode = BlendMode.SrcIn,
            )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 4f,
                center = Offset(
                    size.width / 2f + height / 6f + size.width * offsetMoonDown,
                    size.height / 4f * 1f,
                ),
                blendMode = BlendMode.SrcIn,
            )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 4f,
                center = Offset(
                    size.width / 2f + height / 8f + size.width * offsetMoonDown - size.width,
                    size.height / 4f * 3f,
                ),
                blendMode = BlendMode.SrcIn,
            )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 4f,
                center = Offset(
                    size.width / 2f + height / 8f + size.width * offsetMoonDown,
                    size.height / 4f * 3f,
                ),
                blendMode = BlendMode.SrcIn,
            )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 6f,
                center = Offset(
                    height / 8f + size.width * offsetMoonDown - size.width,
                    size.height / 5f * 1f,
                ),
                blendMode = BlendMode.SrcIn,
            )
            drawCircle(
                color = moonDownColor,
                radius = starRadius.toPx() / 6f,
                center = Offset(
                    height / 8f + size.width * offsetMoonDown,
                    size.height / 5f * 1f,
                ),
                blendMode = BlendMode.SrcIn,
            )
            restoreToCount(checkPoint)
        }

        // 4: bottom shadow
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            drawCircle(
                color = moonBottomShadowColor,
                radius = starRadius.toPx() + starRadius.toPx() * 0.1f,
                center = Offset(size.width / 2f + progressX.toPx(), size.height / 2f),
            )
            drawCircle(
                color = Color.Transparent,
                radius = starRadius.toPx(),
                center = Offset(
                    size.width / 2f - starRadius.toPx() * 0.05f + starRadius.toPx() * 0.005f * offset + progressX.toPx(),
                    size.height / 2f - starRadius.toPx() * 0.1f + starRadius.toPx() * 0.005f * offset,
                ),
                blendMode = BlendMode.SrcIn,
            )
            restoreToCount(checkPoint)
        }
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

