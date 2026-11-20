/**
 * 极坐标取点与角度换算，对应原项目 https://github.com/Jichensheng/Fish_2 里的 `FishDrawable.calculatPoint`，
 * 以及 `PathMeasure.getPosTan` 之后用 `atan2(-tan.y, tan.x)` 取角度的口径（0° 指向 +x）。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 鱼的几何计算：纯函数、无绘制、无 Android 依赖，可直接 JVM 单测。
 *
 * 移植自「灵动的红鲤鱼」的原实现（Canvas 2D 绘制），沿用它的坐标约定：
 * - y 轴朝下（屏幕坐标）；
 * - 角度以 x 轴正方向为 0、顺时针为正，但计算时对 y 做取反，因此
 *   `angle = 90°` 是「屏幕上方」，`angle = 0°` 是「屏幕右侧」。
 *
 * 之所以把数学单独抽出来：原实现里坐标系与角度方向混用（`angle - 180`、`atan2(-dy, dx)`），
 * 改一处就会得到一条倒着游的鱼；抽成纯函数后可以用单测把「鱼头朝向 = 路径切线」锁死。
 */
internal object FishGeometry {

    /** 从 [start] 沿 [angleDegrees] 方向前进 [length] 后到达的点。 */
    fun point(start: Offset, length: Float, angleDegrees: Float): Offset = Offset(
        x = start.x + (cos(Math.toRadians(angleDegrees.toDouble())) * length).toFloat(),
        // y 轴朝下：sin 用 angle-180 抵消方向翻转，与原实现逐点一致
        y = start.y + (sin(Math.toRadians(angleDegrees - 180.0)) * length).toFloat(),
    )

    /**
     * 有符号夹角：以 [center] 为顶点，从 [head] 方向转到 [touch] 方向的角度（度）。
     *
     * 用向量点积求夹角大小、叉积定向符号，因此结果落在 `[-180, 180]`。
     * 原实现直接比较 `direction == 0f`，这里保留同一语义（浮点相等即视为共线）。
     */
    fun includedAngle(center: Offset, head: Offset, touch: Offset): Float {
        val abx = head.x - center.x
        val aby = head.y - center.y
        val acx = touch.x - center.x
        val acy = touch.y - center.y
        val dot = abx * acx + aby * acy
        val denominator = sqrt(abx * abx + aby * aby) * sqrt(acx * acx + acy * acy)
        if (denominator == 0f) return 0f
        // coerce 只为抵消浮点误差（|cos| 略大于 1 时 acos 返回 NaN），不改变方向判定
        val degrees = Math.toDegrees(acos((dot / denominator).coerceIn(-1f, 1f)).toDouble()).toFloat()
        val direction = (center.x - touch.x) * (head.y - touch.y) - (center.y - touch.y) * (head.x - touch.x)
        return when {
            direction == 0f -> if (dot >= 0f) 0f else 180f
            direction > 0f -> -degrees
            else -> degrees
        }
    }

    /** 线段 [start] → [end] 相对 x 轴正方向的角度（用于比较而不是绘制）。 */
    fun lineAngle(start: Offset, end: Offset): Float =
        includedAngle(start, Offset(start.x + 1f, start.y), end)

    /**
     * 转弯用三次贝塞尔的第二控制点：取「鱼身重心 → 鱼头」与「鱼身重心 → 目标点」夹角的角平分线，
     * 距离 1.6 倍头半径。这是原实现让鱼「侧身拐弯」而不是原地平移的关键。
     */
    fun turnControl(fishMiddle: Offset, fishHead: Offset, touch: Offset, headRadius: Float): Offset {
        val half = includedAngle(fishMiddle, fishHead, touch) / 2f
        return point(fishMiddle, 1.6f * headRadius, half + lineAngle(fishMiddle, fishHead))
    }

    /** 三次贝塞尔在参数 [t]（0..1）处的位置。 */
    fun cubic(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
        val u = 1f - t
        val a = u * u * u
        val b = 3f * u * u * t
        val c = 3f * u * t * t
        val d = t * t * t
        return Offset(
            x = a * p0.x + b * p1.x + c * p2.x + d * p3.x,
            y = a * p0.y + b * p1.y + c * p2.y + d * p3.y,
        )
    }

    /**
     * 三次贝塞尔在参数 [t] 处切线的方向角（度），口径与 [point] 一致。
     *
     * 端点处导数可能退化为零向量（控制点重合），此时回退到端点到相邻控制点的方向。
     */
    fun cubicAngle(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Float {
        val u = 1f - t
        val dx = 3f * u * u * (p1.x - p0.x) + 6f * u * t * (p2.x - p1.x) + 3f * t * t * (p3.x - p2.x)
        val dy = 3f * u * u * (p1.y - p0.y) + 6f * u * t * (p2.y - p1.y) + 3f * t * t * (p3.y - p2.y)
        if (dx == 0f && dy == 0f) {
            val fallbackX = if (t < 0.5f) p1.x - p0.x else p3.x - p2.x
            val fallbackY = if (t < 0.5f) p1.y - p0.y else p3.y - p2.y
            return Math.toDegrees(atan2(-fallbackY.toDouble(), fallbackX.toDouble())).toFloat()
        }
        // 与原实现 PathMeasure.getPosTan 后 atan2(-tan.y, tan.x) 的口径一致
        return Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble())).toFloat()
    }

    /** [cubicAngle] 的向量版：给出前进方向单位向量。与 [point] 的口径一致（y 轴朝下取反）。 */
    fun direction(headingDegrees: Float): Offset = Offset(
        x = cos(Math.toRadians(headingDegrees.toDouble())).toFloat(),
        y = -sin(Math.toRadians(headingDegrees.toDouble())).toFloat(),
    )

    /** 由向量求朝向角（度），[direction] 的逆运算。 */
    fun headingOf(vector: Offset): Float =
        Math.toDegrees(atan2(-vector.y.toDouble(), vector.x.toDouble())).toFloat()

    /** 把角度归一到 `(-180, 180]`，用于求「最短转向哪一侧」。 */
    fun normalizeAngle(degrees: Float): Float {
        var value = degrees % 360f
        if (value > 180f) value -= 360f
        if (value <= -180f) value += 360f
        return value
    }

    /** 归一化向量；零向量退化为 +x，避免后续除零得到 NaN。 */
    fun normalize(vector: Offset): Offset {
        val length = sqrt(vector.x * vector.x + vector.y * vector.y)
        return if (length < 0.0001f) Offset(1f, 0f) else Offset(vector.x / length, vector.y / length)
    }
}
