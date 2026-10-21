package com.example.fragmject.core.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** 云朵由 7 段圆叠成：各段半径比例、横向间距、阴影偏移。 */
private val CLOUD_OFFSET_RADIUS = listOf(1f, 0.8f, 0.6f, 0.5f, 0.6f, 0.8f, 0.6f)
private val CLOUD_OFFSET_X = listOf(0, 2, 4, 6, 7, 8, 8)
private val CLOUD_SHADOW_OFFSET_Y = listOf(1f, 2f, 2f, 2f, 1f, 1f, 1f)
private val CLOUD_SHADOW_OFFSET_X = listOf(0f, 0f, 0f, 0f, 0f, 0f, -0.8f)

@Composable
fun Sky(
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    canvasRadius: Dp,
    infiniteTransition: InfiniteTransition,
) {
    val offset1 = infiniteTransition.fluctuating(
        initialValue = 0.95f,
        targetValue = 1.05f,
        durationMillis = remember { getRandom(3000f, 5000f).toInt() },
        label = "offset1",
    )
    val offset2 = infiniteTransition.fluctuating(
        initialValue = 0.9f,
        targetValue = 1.1f,
        durationMillis = remember { getRandom(5000f, 7000f).toInt() },
        label = "offset2",
    )
    val offset3 = infiniteTransition.fluctuating(
        initialValue = 0.85f,
        targetValue = 1.15f,
        durationMillis = remember { getRandom(1000f, 10000f).toInt() },
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
            // 四层同心光晕共用圆心，半径与颜色层层递进
            val center = Offset(offsetX.toPx(), canvasHeight.toPx() / 2f)
            drawSkyLayer(
                lightColor = lightBackgroundColor[0],
                nightColor = nightBackgroundColor[0],
                progress = progress,
                radius = maxRadius * 1.5f,
                center = center,
            )
            drawSkyLayer(
                lightColor = lightBackgroundColor[1],
                nightColor = nightBackgroundColor[1],
                progress = progress,
                radius = (minRadius + (maxRadius - minRadius) / 7f * 4f) * offset1,
                center = center,
            )
            drawSkyLayer(
                lightColor = lightBackgroundColor[2],
                nightColor = nightBackgroundColor[2],
                progress = progress,
                radius = (minRadius + (maxRadius - minRadius) / 7f * 2f) * offset2,
                center = center,
            )
            drawSkyLayer(
                lightColor = lightBackgroundColor[3],
                nightColor = nightBackgroundColor[3],
                progress = progress,
                radius = minRadius * offset3,
                center = center,
            )
        },
    )
}

/** 单层光晕：按昼夜进度插值取色后画圆。 */
private fun DrawScope.drawSkyLayer(
    lightColor: Color,
    nightColor: Color,
    progress: Float,
    radius: Float,
    center: Offset,
) {
    drawCircle(
        color = offsetColor(lightColor, nightColor, progress),
        radius = radius,
        center = center,
    )
}

/** 无限往复的浮动动画；时长由调用方 remember，避免每帧重算随机值。 */
@Composable
private fun InfiniteTransition.fluctuating(
    initialValue: Float,
    targetValue: Float,
    durationMillis: Int,
    label: String,
): Float {
    val value by animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = label,
    )
    return value
}

/** 太阳/月亮切换时云与星星的纵向位移进度：两端各保留 [PER_DISTANCE] 的静止区。 */
internal fun driftProgress(progress: Float): Float = when {
    progress < PER_DISTANCE -> 0f
    progress > 1f - PER_DISTANCE -> 1f
    else -> (progress - PER_DISTANCE) / (1f - PER_DISTANCE * 2f)
}

@Composable
fun Cloud(
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    canvasRadius: Dp,
    infiniteTransition: InfiniteTransition,
) {
    val cloudRadius = canvasRadius - canvasHeight / 10f
    val cloudOffsetX = (canvasWidth - cloudRadius * 1.1f) / 7f
    val cloudOffsetY = canvasHeight / 2f / 10f
    val baseOffsetX = -cloudRadius / 5f
    val baseOffsetY = canvasHeight / 6f
    val cloudShadowOffsetY = -canvasHeight / 8f
    val cloudColor = Color(0xFFFFFFFF)
    val cloudColorShadow = Color(0xFFFFFFFF)
    val animationOffsetX = infiniteTransition.fluctuating(-1f, 1f, 3100, "animationOffsetX")
    val animationOffsetY = infiniteTransition.fluctuating(-1f, 1f, 2900, "animationOffsetY")
    val animationOffsetRadius = infiniteTransition.fluctuating(-1f, 1f, 3000, "animationOffsetRadius")
    // for sun/moon change, this is Cloud move animation
    val progressY = driftProgress(progress)
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
                val shadowDriftX = size.width * 0.05f * animationOffsetX
                val shadowX = size.width - cloudOffsetX.toPx() * i + baseOffsetX.toPx() -
                    baseOffsetX.toPx() * CLOUD_SHADOW_OFFSET_X[i] + shadowDriftX
                val shadowY = size.height / 2f + cloudOffsetY.toPx() * CLOUD_OFFSET_X[i] +
                    baseOffsetY.toPx() + cloudShadowOffsetY.toPx() * CLOUD_SHADOW_OFFSET_Y[i] +
                    size.height / 2f * 0.05f * animationOffsetY
                drawCircle(
                    color = cloudColorShadow,
                    radius = cloudRadius.toPx() * CLOUD_OFFSET_RADIUS[i] +
                        cloudRadius.toPx() * 0.08f * animationOffsetRadius,
                    center = Offset(shadowX, shadowY),
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
                val driftX = size.width * 0.04f * animationOffsetX
                val cloudX = size.width - cloudOffsetX.toPx() * i + baseOffsetX.toPx() + driftX
                val cloudY = size.height / 2f + cloudOffsetY.toPx() * CLOUD_OFFSET_X[i] +
                    baseOffsetY.toPx() + size.height / 2f * 0.04f * animationOffsetY
                drawCircle(
                    color = cloudColor,
                    radius = cloudRadius.toPx() * CLOUD_OFFSET_RADIUS[i] +
                        cloudRadius.toPx() * 0.06f * animationOffsetRadius,
                    center = Offset(cloudX, cloudY),
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
    val progressY = driftProgress(progress)
    for (nightStar in nightStars) {
        // 状态迁移统一由 LaunchedEffect 承担：在组合期写 snapshot 状态会使当前组合立即失效，
        // 触发额外一轮重组（Compose 明确反模式），这里只读取不写入。
        LaunchedEffect(nightStar) {
            if (nightStar.status.value == NightStarStatus.Start) {
                nightStar.status.value = NightStarStatus.Lighting
            }
            if (nightStar.status.value == NightStarStatus.Lighting) {
                val durationMs = getRandom(3000f, 6000f).toLong()
                val halfCycleMs = (durationMs / 4).coerceAtLeast(16) // 每半周期
                val animatable = Animatable(0f)
                // 脉冲闪烁：0→1→0→1→0（4个半周期 = 2次完整闪烁）
                repeat(4) { i ->
                    val target = if (i % 2 == 0) 1f else 0f
                    animatable.animateTo(target, tween(halfCycleMs.toInt(), easing = LinearEasing))
                    nightStar.alpha.floatValue = animatable.value
                }
                nightStar.alpha.floatValue = 0f
                nightStar.status.value = NightStarStatus.End
                nightStars.remove(nightStar)
                if (nightStars.size < 10) {
                    nightStars.add(getRandomStart())
                }
            }
        }
        // a simple path to draw a little star
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
                val baseY = (canvasHeight.toPx() - starRadius.toPx() * 2f) / 2f
                val temp = Pair(
                    baseY + (canvasWidth.toPx() / 2f - baseY) * nightStar.x.floatValue,
                    baseY + (buttonHeight.toPx() - baseY) * nightStar.y.floatValue,
                )
                // you can check the start position is not too nearly with other stars
                val x = temp.first
                val y = temp.second
                val radius =
                    canvasHeight.toPx() / 30f + (canvasHeight.toPx() / 60f) * nightStar.radius.floatValue
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
