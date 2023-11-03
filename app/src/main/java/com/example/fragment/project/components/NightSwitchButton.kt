package com.example.fragment.project.components

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.SwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 掘金上看到 clwater 写的效果很棒，非常适合用来做暗夜模式
 * 基于此封装成 NightSwitchButton 方便使用
 * 文章地址：https://juejin.cn/post/7225454746949615673
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NightSwitchButton(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight
        val canvasRadius = canvasHeight / 2f
        val starSize = canvasRadius * 0.9f
        val swipeableState = rememberSwipeableStateFor(checked, onCheckedChange ?: {})
        val minBound = 0f
        val maxBound = with(LocalDensity.current) { (canvasWidth - starSize).toPx() }
        Box(
            modifier = Modifier
                .toggleable(
                    value = checked,
                    onValueChange = {
                        coroutineScope.launch {
                            swipeableState.animateTo(it)
                            onCheckedChange?.invoke(it)
                        }
                    },
                    role = Role.Switch,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                .swipeable(
                    state = swipeableState,
                    anchors = mapOf(minBound to false, maxBound to true),
                    thresholds = { _, _ -> FractionalThreshold(0.5f) },
                    orientation = Orientation.Horizontal
                )
        ) {
            val progress = if (!swipeableState.progress.from && !swipeableState.progress.to) {
                0f
            } else if (swipeableState.progress.from && !swipeableState.progress.to) {
                1f - swipeableState.progress.fraction
            } else {
                swipeableState.progress.fraction
            }
            Sky(progress, canvasWidth, canvasHeight, canvasRadius)
            Cloud(progress, canvasWidth, canvasHeight, canvasRadius)
            Stars(progress, canvasWidth, canvasHeight, canvasRadius)
            SunAndMoon(progress, canvasWidth, canvasHeight, canvasRadius)
        }
    }
}

@Composable
@ExperimentalMaterialApi
internal fun <T : Any> rememberSwipeableStateFor(
    value: T,
    onValueChange: (T) -> Unit,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec
): SwipeableState<T> {
    val swipeableState = remember {
        SwipeableState(
            initialValue = value,
            animationSpec = animationSpec,
            confirmStateChange = { true }
        )
    }
    val forceAnimationCheck = remember { mutableStateOf(false) }
    LaunchedEffect(value, forceAnimationCheck.value) {
        if (value != swipeableState.currentValue) {
            swipeableState.animateTo(value)
        }
    }
    DisposableEffect(swipeableState.currentValue) {
        if (value != swipeableState.currentValue) {
            onValueChange(swipeableState.currentValue)
            forceAnimationCheck.value = !forceAnimationCheck.value
        }
        onDispose { }
    }
    return swipeableState
}

@Composable
fun Sky(
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    canvasRadius: Dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Sky")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = getRandom(3000f, 5000f).toInt(),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offset1",
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = getRandom(5000f, 7000f).toInt(),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offset2",
    )
    val offset3 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = getRandom(1000f, 10000f).toInt(),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offset3",
    )
    val starRadius = canvasRadius * 0.9f
    val starMove = canvasWidth - (canvasHeight - starRadius * 2f) - starRadius * 2f
    val offsetX = canvasHeight / 2f + starMove * progress
    Canvas(
        modifier = Modifier
            .width(canvasWidth)
            .height(canvasHeight)
            .clip(RoundedCornerShape(canvasRadius))
            .clipToBounds(),
        onDraw = {
            val maxRadius = canvasWidth.toPx() - canvasRadius.toPx() * 1.5f
            val minRadius = maxRadius * 0.3f
            drawCircle(
                color = offsetColor(
                    lightBackgroundColor[0],
                    nightBackgroundColor[0],
                    progress,
                ),
                radius = maxRadius * 1.5f,
                center = Offset(
                    offsetX.toPx(),
                    canvasHeight.toPx() / 2f,
                ),
            )
            drawCircle(
                color = offsetColor(
                    lightBackgroundColor[1],
                    nightBackgroundColor[1],
                    progress,
                ),
                radius = (minRadius + (maxRadius - minRadius) / 7f * 4f) * offset1,
                center = Offset(
                    offsetX.toPx(),
                    canvasHeight.toPx() / 2f,
                ),
            )
            drawCircle(
                color = offsetColor(
                    lightBackgroundColor[2],
                    nightBackgroundColor[2],
                    progress,
                ),
                radius = (minRadius + (maxRadius - minRadius) / 7f * 2f) * offset2,
                center = Offset(
                    offsetX.toPx(),
                    canvasHeight.toPx() / 2f,
                ),
            )
            drawCircle(
                color = offsetColor(
                    lightBackgroundColor[3],
                    nightBackgroundColor[3],
                    progress,
                ),
                radius = minRadius * offset3,
                center = Offset(
                    offsetX.toPx(),
                    canvasHeight.toPx() / 2f,
                ),
            )
        },
    )
}

@Composable
fun Cloud(
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    canvasRadius: Dp,
) {
    val cloudRadius = canvasRadius - canvasHeight / 10f
    val cloudOffsetX = (canvasWidth - cloudRadius * 1.1f) / 7f
    val cloudOffsetY = canvasHeight / 2f / 10f
    val baseOffsetX = -cloudRadius / 5f
    val baseOffsetY = canvasHeight / 6f
    val cloudShadowOffsetY = -canvasHeight / 8f
    val cloudColor = Color(0xFFFFFFFF)
    val cloudColorShadow = Color(0xFFFFFFFF)
    // this list is cloud(shadow) offset
    val offsetRadius = listOf(1f, 0.8f, 0.6f, 0.5f, 0.6f, 0.8f, 0.6f)
    val offsetX = listOf(0, 2, 4, 6, 7, 8, 8)
    val shadowOffsetY = listOf(1f, 2f, 2f, 2f, 1f, 1f, 1f)
    val shadowOffsetX = listOf(0f, 0f, 0f, 0f, 0f, 0f, -0.8f)
    val infiniteTransition = rememberInfiniteTransition(label = "Cloud")
    val animationOffsetX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3100,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "animationOffsetX",
    )
    val animationOffsetY by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2900,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "animationOffsetY",
    )
    val animationOffsetRadius by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "animationOffsetRadius",
    )
    // for sun/moon change, this is Cloud move animation
    val progressY = if (progress < perDistance) {
        0f
    } else if (progress > (1f - perDistance)) {
        1f
    } else {
        (progress - perDistance) / (1f - perDistance * 2f)
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(canvasRadius))) {
        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasHeight)
                .offset(y = canvasHeight * progressY)
                .alpha(0.5f),
        ) {
            // 白云2
            for (i in 0..6) {
                drawCircle(
                    color = cloudColorShadow,
                    radius = cloudRadius.toPx() * offsetRadius[i] + cloudRadius.toPx() * 0.08f * animationOffsetRadius,
                    center = Offset(
                        size.width - cloudOffsetX.toPx() * i + baseOffsetX.toPx() - baseOffsetX.toPx() * shadowOffsetX[i] + size.width * 0.05f * animationOffsetX,
                        size.height / 2f + cloudOffsetY.toPx() * offsetX[i] + baseOffsetY.toPx() + cloudShadowOffsetY.toPx() * shadowOffsetY[i] + size.height / 2f * 0.05f * animationOffsetY,
                    ),
                )
            }
        }
        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasHeight)
                .offset(y = canvasHeight * progressY),
        ) {
            // 白云1
            for (i in 0..6) {
                drawCircle(
                    color = cloudColor,
                    radius = cloudRadius.toPx() * offsetRadius[i] + cloudRadius.toPx() * 0.06f * animationOffsetRadius,
                    center = Offset(
                        size.width - cloudOffsetX.toPx() * i + baseOffsetX.toPx() + size.width * 0.04f * animationOffsetX,
                        size.height / 2f + cloudOffsetY.toPx() * offsetX[i] + baseOffsetY.toPx() + size.height / 2f * 0.04f * animationOffsetY,
                    ),
                )
            }
        }
    }
}

@Composable
fun Stars(
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    canvasRadius: Dp
) {
    val nightStars = remember { mutableStateListOf<NightStar>() }
    if (nightStars.isEmpty()) {
        nightStars.clear()
        for (i in 0..10) {
            nightStars.add(getRandomStart())
        }
    }
    // for sun/moon change, this is NightStar move animation
    val progressY = if (progress < perDistance) {
        0f
    } else if (progress > (1f - perDistance)) {
        1f
    } else {
        (progress - perDistance) / (1f - perDistance * 2f)
    }
    for (nightStar in nightStars) {
        // if NightStar is not lighting, then start lighting animation
        if (nightStar.status.value == NightStarStatus.Start) {
            nightStar.status.value = NightStarStatus.Lighting
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.duration = getRandom(3000f, 6000f).toLong()
            valueAnimator.repeatMode = ValueAnimator.REVERSE
            valueAnimator.repeatCount = 2
            valueAnimator.interpolator = DecelerateInterpolator()
            valueAnimator.addUpdateListener {
                val value = it.animatedValue as Float
                nightStar.alpha.floatValue = value
            }
            valueAnimator.addListener {
                it.doOnEnd {
                    nightStar.status.value = NightStarStatus.End
                    nightStars.remove(nightStar)
                    if (nightStars.size < 10) {
                        nightStars.add(getRandomStart())
                    }
                }
            }
            valueAnimator.start()
        }
        // an simple path to draw a little star
        Box(modifier = Modifier.clip(RoundedCornerShape(canvasRadius))) {
            Canvas(
                modifier = Modifier
                    .width(canvasWidth)
                    .height(canvasHeight)
                    .offset(y = -canvasHeight + canvasHeight * progressY)
                    .alpha(nightStar.alpha.floatValue),
            ) {
                val starRadius = canvasRadius * 0.9f
                val buttonHeight = canvasHeight - canvasHeight / 10f * 2
                val temp = Pair(
                    (canvasHeight.toPx() - starRadius.toPx() * 2f) / 2f + (canvasWidth.toPx() / 2f - (canvasHeight.toPx() - starRadius.toPx() * 2f) / 2f) * nightStar.x.value,
                    (canvasHeight.toPx() - starRadius.toPx() * 2f) / 2f + (buttonHeight.toPx() - (canvasHeight.toPx() - starRadius.toPx() * 2f) / 2f) * nightStar.y.value,
                )
                // you can check the start position is not too nearly with other stars
                val x = temp.first
                val y = temp.second
                val radius =
                    canvasHeight.toPx() / 30f + (canvasHeight.toPx() / 60f) * nightStar.radius.value
                val path = Path()
                path.moveTo(x, y + radius)
                path.lineTo(x + radius / 3f, y + radius / 3f)
                path.lineTo(x + radius, y)
                path.lineTo(x + radius / 3f, y - radius / 3f)
                path.lineTo(x, y - radius)
                path.lineTo(x - radius / 3f, y - radius / 3f)
                path.lineTo(x - radius, y)
                path.lineTo(x - radius / 3f, y + radius / 3f)
                path.close()
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = radius / 2f),
                )
            }
        }
    }
}

@Composable
fun SunAndMoon(
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    canvasRadius: Dp,
) {
    var initProgress by remember { mutableFloatStateOf(0f) }
    if (progress <= 0f) {
        initProgress = 0f
    } else if (progress >= 1f) {
        initProgress = 1f
    }
    val mSunCloudRadius = canvasRadius - canvasHeight / 10f
    val starRadius = mSunCloudRadius * 0.9f
    val starMove = canvasWidth - (canvasHeight - starRadius * 2f) - starRadius * 2f
    Box(
        modifier = Modifier
            .height(starRadius * 2)
            .width(starRadius * 2),
    ) {
        if (progress >= initProgress) {
            Sun(progress, false, canvasHeight, canvasRadius, starRadius, starMove)
        }
        Moon(progress, progress < initProgress, canvasHeight, canvasRadius, starRadius, starMove)
        if (progress < initProgress) {
            Sun(progress, true, canvasHeight, canvasRadius, starRadius, starMove)
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
    starMove: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Sun")
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
                x = (canvasHeight - starRadius * 2f) / 2f + starMove * progress,
                y = (canvasHeight - starRadius * 2f) / 2f,
            )
            .graphicsLayer(alpha = 0.99f)
            .clip(RoundedCornerShape(canvasRadius))
            .clipToBounds()
            .width(starRadius * 2f)
            .height(starRadius * 2f),
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
    starMove: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Moon")
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
                x = (canvasHeight - starRadius * 2f) / 2f + starMove * progress,
                y = (canvasHeight - starRadius * 2f) / 2f,
            )
            .graphicsLayer(alpha = 0.99f)
            .clip(RoundedCornerShape(canvasRadius))
            .clipToBounds()
            .width(starRadius * 2f)
            .height(starRadius * 2f)
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

private const val perDistance = 0.2f

private val lightBackgroundColor = listOf(
    Color(0xFF1565C0),
    Color(0xFF1E88E5),
    Color(0xFF2196F3),
    Color(0xFF42A5F5),
)
private val nightBackgroundColor = listOf(
    Color(0xFF1C1E2B),
    Color(0xFF2E323C),
    Color(0xFF3E424E),
    Color(0xFF4F555D),
)

private val sunColor = Color(0xFFFFD54F)
private val sunColorDeep = Color(0xFFFFA726)
private val sunTopShadowColor = Color(0xCCFFFFFF)
private val sunBottomShadowColor = Color(0x80827717)

private val moonColor = Color(0xFFC3C9D1)
private val moonTopShadowColor = Color(0xCCFFFFFF)
private val moonBottomShadowColor = Color(0xFF5E5E5E)
private val moonDownColor = Color(0xFF73777E)

/**
 * A tool for get 2 colors offset color
 */
private fun offsetColor(
    colorStart: Color,
    colorEnd: Color,
    progress: Float,
    perDistance: Float = 0.2f
): Color {
    val offsetColor = if (progress < perDistance) {
        0f
    } else if (progress > (1 - perDistance)) {
        1f
    } else {
        (progress - perDistance) * (1 / (1 - perDistance * 2))
    }
    val red = (((colorStart.red + (colorEnd.red - colorStart.red) * offsetColor) * 0xFF).toInt())
    val green =
        (((colorStart.green + (colorEnd.green - colorStart.green) * offsetColor) * 0xFF).toInt())
    val blue =
        (((colorStart.blue + (colorEnd.blue - colorStart.blue) * offsetColor) * 0xFF).toInt())
    val color =
        ((0xFF and 0xFF) shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)
    return Color(color)
}

private fun getRandom(min: Float, max: Float): Float {
    return Random.nextFloat() * (max - min) + min
}

/**
 * Night Star Info
 */
class NightStar {
    // position and radius
    var x = mutableFloatStateOf(0f)
    var y = mutableFloatStateOf(0f)
    var radius = mutableFloatStateOf(0f)
    var alpha = mutableFloatStateOf(0f)
    var status = mutableStateOf(NightStarStatus.Start)
}

/**
 * Night Star Status
 */
enum class NightStarStatus {
    Start,
    End,
    Lighting,
}