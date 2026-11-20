/**
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 绘制层。
 *
 * 原实现直接在 `FishDrawable.onDraw` 里画，并用 `saveLayerAlpha(0.94)` 给整层加半透明。
 * 这里不做离屏图层：把 alpha 乘进各部件颜色，省掉一次全屏 saveLayer。
 */
package com.example.fragmject.core.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import com.example.fragmject.core.designsystem.AppColors
import kotlin.math.cos
import kotlin.math.sin

/** 水花射线数量。 */
private const val SPLASH_RAYS = 6

/**
 * 水面层：涟漪（可多圈）+ 落水水花。
 *
 * 两者互补：涟漪是"水平扩散的圈"，水花是"向上溅起的水柱"，叠加起来才像真的砸进水里。
 */
@Composable
internal fun FishRippleLayer(motion: FishMotion, metrics: FishMetrics, opacity: Float) {
    // 没有涟漪/水花时整层不挂载：全屏 Canvas 即便不重绘，也会参与每次显示列表重录
    val hasFx by remember(motion) {
        derivedStateOf { motion.ripples.isNotEmpty() || motion.splash != null }
    }
    if (!hasFx) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val fade = opacity.coerceIn(0f, 1f)
        motion.ripples.forEach { ripple ->
            drawCircle(
                color = AppColors.blue.copy(alpha = ripple.alpha * fade),
                radius = ripple.radius,
                center = ripple.center,
                style = Stroke(width = metrics.rippleStrokePx),
            )
        }
        motion.splash?.let { splash ->
            drawSplash(splash, metrics.headRadiusPx, AppColors.blue.copy(alpha = splash.alphaScale() * fade))
        }
    }
}

private fun FishSplash.alphaScale(): Float = 0.35f * (1f - progress.coerceIn(0f, 1f))

/** 从水面点向上方辐射几条短线，随进度变长变淡。 */
private fun DrawScope.drawSplash(splash: FishSplash, headRadius: Float, color: Color) {
    val progress = splash.progress.coerceIn(0f, 1f)
    if (color.alpha <= 0.001f) return
    val inner = headRadius * 0.7f
    val outer = inner + headRadius * 1.5f * progress
    repeat(SPLASH_RAYS) { index ->
        // 只朝上半圈散开（-160°~20°），否则像太阳的放射光
        val degrees = -160f + index * (180f / (SPLASH_RAYS - 1))
        val radians = Math.toRadians(degrees.toDouble())
        val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
        drawLine(
            color = color,
            start = splash.center + direction * inner,
            end = splash.center + direction * outer,
            strokeWidth = headRadius * 0.12f,
        )
    }
}

/**
 * 鱼本体：残影 → 影子 → 鱼（含抖动 / 上抛 / 跃起放大 / 沿朝向的挤压拉伸）。
 *
 * 节点位置锚在 `middle`，所以画在节点中心；残影与影子是**世界坐标**，
 * 需要换算到节点局部（`world - middle + nodeCenter`）。
 */
@Composable
internal fun FishBody(motion: FishMotion, metrics: FishMetrics, color: Color, opacity: Float) {
    // 路径缓冲随组件复用：每帧重建 4 段轮廓但不重新分配 Path
    val paths = remember { FishPaths() }
    Canvas(
        modifier = Modifier
            .size(metrics.nodeSize)
            .graphicsLayer {
                translationX = motion.middle.x - metrics.nodeSizePx / 2f
                translationY = motion.middle.y - metrics.nodeSizePx / 2f
                // 穿越：淡出 → 不可见 → 淡入带 1.12 倍回弹（图层变换，不触发重绘）。
                // 注意这里**不带设置里的透明度**：那个已经在各部件的颜色里乘过一次，
                // 两层都乘会让 0.6 显示成 0.36（实测滑杆越往左越不像话）。
                val teleportElapsed = motion.teleportElapsed
                alpha = FishTeleport.alpha(teleportElapsed)
                val teleportScale = FishTeleport.scale(teleportElapsed)
                scaleX = teleportScale
                scaleY = teleportScale
            }
            // 纯装饰元素：不参与无障碍焦点，否则 TalkBack 会停在一条鱼上
            .semantics { hideFromAccessibility() },
    ) {
        if (motion.middle == Offset.Zero) return@Canvas
        val nodeCenter = Offset(size.width / 2f, size.height / 2f)
        val fade = opacity.coerceIn(0f, 1f)
        drawGhosts(motion, metrics, nodeCenter, fade, paths)
        drawGroundShadow(motion, metrics, nodeCenter, fade)

        // 上抛（负 y）、打盹浮动与受惊抖动合并成一个位移
        val liftPx = (motion.leapLiftR - motion.bobR) * metrics.headRadiusPx
        translate(left = motion.jitter.x, top = -liftPx + motion.jitter.y) {
            scale(motion.leapScale, motion.leapScale, pivot = nodeCenter) {
                // 先把坐标系转到「运动方向 = +x」，挤压拉伸才沿前进方向而不是屏幕 x 轴
                rotate(degrees = -motion.angle, pivot = nodeCenter) {
                    scale(motion.stretchLong, motion.stretchShort, pivot = nodeCenter) {
                        drawFish(
                            paths = paths,
                            middle = nodeCenter,
                            headRadius = metrics.headRadiusPx,
                            // 坐标系已旋转，这里按 +x 画
                            mainAngle = 0f,
                            phaseDegrees = motion.phase,
                            vigor = motion.vigor,
                            color = color,
                            opacity = motion.opacity * fade,
                        )
                    }
                }
            }
        }
    }
}

/** 残影：用历史位置画 1~2 个淡影（世界坐标 → 节点局部）。 */
private fun DrawScope.drawGhosts(
    motion: FishMotion,
    metrics: FishMetrics,
    nodeCenter: Offset,
    fade: Float,
    paths: FishPaths,
) {
    motion.ghosts.forEach { ghost ->
        val delta = ghost.center - motion.middle
        drawFish(
            paths = paths,
            middle = nodeCenter + delta,
            headRadius = metrics.headRadiusPx,
            mainAngle = ghost.heading,
            phaseDegrees = ghost.phaseDegrees,
            vigor = 0.9f,
            opacity = ghost.alpha * fade,
        )
    }
}

/**
 * 地面影子：留在**起跳的水面点**，朝固定光源反方向偏移并缩小变淡。
 *
 * 这是俯视下"离开水面"最硬的线索——影子跟鱼重合时看不出高度，分离了才读得出来。
 */
private fun DrawScope.drawGroundShadow(
    motion: FishMotion,
    metrics: FishMetrics,
    nodeCenter: Offset,
    fade: Float,
) {
    if (motion.shadowAlpha <= 0.001f) return
    val anchor = if (motion.shadowCenter == Offset.Zero) {
        nodeCenter
    } else {
        nodeCenter + (motion.shadowCenter - motion.middle)
    }
    val lightDirection = Offset(0.62f, 0.78f)
    val center = anchor + lightDirection * (motion.shadowOffsetR * metrics.headRadiusPx)
    val rx = metrics.headRadiusPx * 1.35f * motion.shadowScale
    val ry = metrics.headRadiusPx * 0.9f * motion.shadowScale
    drawOval(
        color = Color.Black.copy(alpha = motion.shadowAlpha * fade),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f),
    )
}
