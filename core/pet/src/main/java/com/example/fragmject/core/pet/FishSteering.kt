/**
 * 转向行为：seek + arrive + 角速度限幅。
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * **不是**原项目的内容：原实现用 `ObjectAnimator` 沿一条三次贝塞尔路径匀速移动 2s，
 * 没有"转向"这个概念。这里参考 Reynolds《Steering Behaviors For Autonomous Characters》重写，
 * 为的是让鱼能连续改目标而不出现"一段一段起停"。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset

/** 判定「有方向」的距离阈值（px）：低于它认为鱼已在目标点上，不再修正朝向。 */
private const val HEADING_EPSILON = 0.001f

/**
 * 转向行为（Craig Reynolds steering behaviors）的纯函数实现。
 *
 * ============================ 为什么不用「每段一条贝塞尔」============================
 * 上一版是「到点 → 重新起一条三次贝塞尔 → 匀速走完」。这种做法有两个天生的僵硬来源：
 * 1. 每一段的首尾速度都不连续，观感是「游一下、停一下」；
 * 2. 朝向来自路径切线，路径一换就瞬间改朝向，没有转向过程。
 *
 * 换成转向行为后：
 * - **Seek**：期望速度指向目标，转向量取「期望朝向 − 当前朝向」的最短旋转；
 * - **限幅**：角速度有上限（`maxTurnRate`），于是任何突变都变成一段弧线；
 * - **Arrive**：进入 [FishSwimTuning.slowRadius] 后按距离线性减速，不会冲过目标来回打转；
 * - **Wander**：目标点由调用方随机刷新（本文件不关心怎么选），转向过程自然平滑。
 *
 * 这些都是纯函数，可以直接对「是否会收敛、是否超速、是否超角速度」写单测。
 * 参考：Reynolds, *Steering Behaviors For Autonomous Characters*（1999）。
 *
 * 坐标/角度口径与 [FishGeometry] 完全一致：y 轴朝下，0° 指向屏幕右侧，90° 指向屏幕上方。
 */
internal data class FishSwimState(
    val position: Offset,
    val heading: Float,
    val speed: Float,
)

/**
 * 一条鱼的游动参数，全部按「每秒」口径给出。
 *
 * @param maxSpeed 巡航速度上限（px/s）
 * @param maxTurnRate 最大角速度（度/s）：鱼的转弯半径 = speed / maxTurnRate
 * @param acceleration 加速上限（px/s²）
 * @param deceleration 减速上限（px/s²）
 * @param slowRadius 进入该半径后开始减速（arrive 行为）
 * @param arriveRadius 到达判定半径
 * @param minSpeed 巡航下限：低于它鱼会「僵住」，摆尾再快也不像在游
 */
internal data class FishSwimTuning(
    val maxSpeed: Float,
    val maxTurnRate: Float,
    val acceleration: Float,
    val deceleration: Float,
    val slowRadius: Float,
    val arriveRadius: Float,
    val minSpeed: Float,
)

/** 单步结果。[turnRate] 供摆尾幅度使用：转得急时鱼本来就会甩得更狠。 */
internal data class FishSwimStep(
    val state: FishSwimState,
    val turnRate: Float,
    val arrived: Boolean,
)

/**
 * 推进一步（[dtSeconds] 通常是 1/60）。
 *
 * 顺序固定为「转向 → 调速 → 位移」，与 Reynolds 的三步（steering → velocity → position）一致；
 * 先位移再转向会让鱼在急转时向外飘出一小段，看起来像甩尾失控。
 */
internal fun FishSwimState.step(
    towards: Offset,
    dtSeconds: Float,
    tuning: FishSwimTuning,
    speedScale: Float = 1f,
): FishSwimStep {
    if (dtSeconds <= 0f) return FishSwimStep(this, 0f, false)
    val toTarget = towards - position
    val distance = toTarget.getDistance()

    // ---- 转向：最短旋转 + 角速度限幅 ----
    // 距离为 0 时 headingOf 会退化成 atan2(0, 0) = 0°，会让悬停中的鱼无故转向右侧，
    // 因此只在真正有方向时求期望朝向
    val desiredHeading = if (distance > HEADING_EPSILON) FishGeometry.headingOf(toTarget) else heading
    val maxDelta = tuning.maxTurnRate * dtSeconds
    val delta = FishGeometry.normalizeAngle(desiredHeading - heading).coerceIn(-maxDelta, maxDelta)
    val newHeading = FishGeometry.normalizeAngle(heading + delta)
    val turnRate = delta / dtSeconds

    // ---- 调速：arrive 减速 + 加/减速限幅 ----
    val cruiseSpeed = tuning.maxSpeed * speedScale.coerceAtLeast(0.1f)
    val desiredSpeed = when {
        distance <= tuning.arriveRadius -> 0f
        distance < tuning.slowRadius -> cruiseSpeed * (distance / tuning.slowRadius)
        else -> cruiseSpeed
    }.coerceAtLeast(if (distance > tuning.arriveRadius) tuning.minSpeed else 0f)
    val speedDelta = desiredSpeed - speed
    val speedLimit = (if (speedDelta >= 0f) tuning.acceleration else tuning.deceleration) * dtSeconds
    val newSpeed = (speed + speedDelta.coerceIn(-speedLimit, speedLimit)).coerceAtLeast(0f)

    // ---- 位移：朝新朝向走 ----
    val newPosition = position + FishGeometry.direction(newHeading) * (newSpeed * dtSeconds)
    val state = FishSwimState(position = newPosition, heading = newHeading, speed = newSpeed)
    return FishSwimStep(state = state, turnRate = turnRate, arrived = distance <= tuning.arriveRadius)
}
