/**
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 活动区域的几何工具。
 *
 * 原实现里鱼的中心点是固定的 `middlePoint`，不存在"活动边界"；
 * 这些函数是为常驻宠物新增的（夹紧边界、求边缘落点、随机取点）。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.random.Random

/**
 * 活动区域的几何工具：安全区夹紧、**允许游出屏幕**的软边界、边缘落点、随机取点。
 *
 * 这里有两套边界，刻意分开：
 * - [clamp]：**目标点/落点**用的安全区（留 margin），保证"要去的地方"看得见；
 * - [clampOutside]：**运动积分**用的软边界（允许越界 overshoot），鱼能游出屏幕一半再回来。
 *
 * 之前两者共用同一个 margin，鱼被死死框在屏幕内，失去了"游出去"的观感。
 */
internal object FishSpace {

    /** 中心点是否还在屏幕矩形内（出界时不安排"要给人看"的行为，例如打盹/绕圈/跃起）。 */
    fun isInside(point: Offset, bounds: Size): Boolean =
        point.x in 0f..bounds.width && point.y in 0f..bounds.height

    /** 夹到安全区：四周留 [margin]（鱼的半身 + 尾鳍），不会被画到容器外。 */
    fun clamp(point: Offset, bounds: Size, margin: Float): Offset = Offset(
        x = point.x.coerceIn(margin, (bounds.width - margin).coerceAtLeast(margin)),
        y = point.y.coerceIn(margin, (bounds.height - margin).coerceAtLeast(margin)),
    )

    /**
     * 软边界：允许中心点越界 [overshoot] 像素。
     *
     * 鱼身比中心点宽，中心刚越过边缘时仍有半个身子在屏内——这才是"游出去"的样子；
     * 上限卡在 [overshoot] 是为了不让它彻底消失、找不回来。
     */
    fun clampOutside(point: Offset, bounds: Size, overshoot: Float): Offset = Offset(
        x = point.x.coerceIn(-overshoot, bounds.width + overshoot),
        y = point.y.coerceIn(-overshoot, bounds.height + overshoot),
    )

    /** 屏幕外的目标点：随机一条边，落在边外 [overshoot] 处（巡航时偶尔用，鱼会出界一趟再回来）。 */
    fun randomEdgeTarget(
        bounds: Size,
        overshoot: Float,
        random: Random = Random,
    ): Offset = when (random.nextInt(EDGE_COUNT)) {
        0 -> Offset(random.nextFloat() * bounds.width, -overshoot)
        1 -> Offset(random.nextFloat() * bounds.width, bounds.height + overshoot)
        2 -> Offset(-overshoot, random.nextFloat() * bounds.height)
        else -> Offset(bounds.width + overshoot, random.nextFloat() * bounds.height)
    }

    /** 把方向延伸到边缘上的点：受惊"贴边躲避"用它，鱼会朝边界跑而不是飘向随机点。 */
    fun edgePoint(from: Offset, direction: Offset, bounds: Size, margin: Float): Offset {
        val unit = FishGeometry.normalize(direction)
        return clamp(from + unit * (bounds.width + bounds.height), bounds, margin)
    }

    fun randomPoint(bounds: Size, margin: Float, random: Random = Random): Offset = Offset(
        x = margin + random.nextFloat() * (bounds.width - margin * 2f).coerceAtLeast(1f),
        y = margin + random.nextFloat() * (bounds.height - margin * 2f).coerceAtLeast(1f),
    )

    fun randomDirection(random: Random = Random): Offset {
        val angle = random.nextFloat() * TWO_PI
        return Offset(kotlin.math.cos(angle), kotlin.math.sin(angle))
    }

    private const val TWO_PI = 6.2831855f
    private const val EDGE_COUNT = 4
}
