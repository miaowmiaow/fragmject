package com.example.fragmject.core.ui.components

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * NightSwitchButton 的配色与几何常量。
 *
 * 与绘制代码分开：调色/数值改动频率高，独立成文件避免在 800 行的绘制逻辑里翻找。
 */
internal const val PER_DISTANCE = 0.2f

internal val lightBackgroundColor = listOf(
    Color(0xFF1565C0),
    Color(0xFF1E88E5),
    Color(0xFF2196F3),
    Color(0xFF42A5F5),
)
internal val nightBackgroundColor = listOf(
    Color(0xFF1C1E2B),
    Color(0xFF2E323C),
    Color(0xFF3E424E),
    Color(0xFF4F555D),
)

internal val sunColor = Color(0xFFFFD54F)
internal val sunColorDeep = Color(0xFFFFA726)
internal val sunTopShadowColor = Color(0xCCFFFFFF)
internal val sunBottomShadowColor = Color(0x80827717)

internal val moonColor = Color(0xFFC3C9D1)
internal val moonTopShadowColor = Color(0xCCFFFFFF)
internal val moonBottomShadowColor = Color(0xFF5E5E5E)
internal val moonDownColor = Color(0xFF73777E)

/**
 * A tool for get 2 colors offset color
 */
internal fun offsetColor(
    colorStart: Color,
    colorEnd: Color,
    progress: Float,
    perDistance: Float = PER_DISTANCE
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

internal fun getRandom(min: Float, max: Float): Float {
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
