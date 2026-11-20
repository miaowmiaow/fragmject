package com.example.fragmject.core.pet

/**
 * 受惊：**僵直 → 贴边逃逸 → 回头张望**。
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 原项目只有"点哪游哪"，没有惊吓反应；这是为常驻宠物新增的。
 *
 * 为什么先僵直：真实的惊跳反射有一拍"愣住"（速度归零、肌肉绷紧），
 * 少了它只是"突然加速"，读不出"被吓到"。僵直期间尾巴仍在抖（`vigor` 拉满）。
 */

/** 受惊的三段。 */
internal enum class StartlePhase { FREEZE, ESCAPE, GLANCE }

/**
 * 受惊时间线（纯函数，可单测）。
 *
 * 这里是**唯一**的时间来源：`FishSwimRuntime` 只按它算出的三个绝对时刻做分支判断，
 * 测试则直接验证阶段边界——过去这些常量散在运行时里，改一个值就可能让"回头"整段消失，
 * 而真机上要盯很久才看得出来。
 */
internal object StartleTimeline {

    /** 整段时长（之后进入恢复段 `RECOVER_SECONDS`）。 */
    const val TOTAL_SECONDS = 0.9f

    /** 僵直时长：太短读不出来，太长会像卡住。 */
    const val FREEZE_SECONDS = 0.10f

    /** 末尾"回头张望"的时长。 */
    const val GLANCE_SECONDS = 0.22f

    /** 起手的挤压拉伸时长。 */
    const val STRETCH_SECONDS = 0.22f

    /** 回头张望的起点（相对整段开头的秒数）。 */
    const val GLANCE_START_SECONDS = TOTAL_SECONDS - GLANCE_SECONDS

    fun phaseAt(elapsedSeconds: Float): StartlePhase = when {
        elapsedSeconds < FREEZE_SECONDS -> StartlePhase.FREEZE
        elapsedSeconds < GLANCE_START_SECONDS -> StartlePhase.ESCAPE
        else -> StartlePhase.GLANCE
    }

    /** 各段的水平速度倍率（相对巡航）：僵直 0、逃逸 1.9、"回头看"时减速到 0.35。 */
    fun speedFactorAt(phase: StartlePhase): Float = when (phase) {
        StartlePhase.FREEZE -> 0f
        StartlePhase.ESCAPE -> 1.9f
        StartlePhase.GLANCE -> 0.35f
    }
}

/**
 * 受惊的每帧推进。
 *
 * 从 `FishSwimRuntime` 里搬出来（那里已 445 行、逼近规则 28 的 500 行上限）；
 * 用同包扩展函数而不是独立类，是因为它只写运行时的移动状态（目标/朝向/速度），
 * 没必要为它引入一层抽象。
 */
internal fun FishSwimRuntime.updateStartledFrame(dtSeconds: Float, maxSpeed: Float) {
    val phase = StartleTimeline.phaseAt(elapsedSeconds - (startleUntil - StartleTimeline.TOTAL_SECONDS))
    when (phase) {
        StartlePhase.FREEZE -> approachSpeed(StartleTimeline.speedFactorAt(phase) * maxSpeed, maxSpeed * 14f * dtSeconds)

        StartlePhase.ESCAPE -> {
            // 朝远离威胁点的方向逃，并贴到最近的边缘（而不是飘向随机点）
            val away = bodyCenter - startleFrom
            navTarget = FishSpace.edgePoint(
                from = bodyCenter,
                direction = if (away.getDistance() < 0.001f) FishSpace.randomDirection() else away,
                bounds = bounds,
                margin = marginPx,
            )
            steerTo(navTarget, dtSeconds, fastTurn = true)
            approachSpeed(StartleTimeline.speedFactorAt(phase) * maxSpeed, maxSpeed * 9f * dtSeconds)
        }

        StartlePhase.GLANCE -> {
            // 回头：把朝向转回威胁点，同时大幅减速（"跑了但还在看"）
            steerTo(startleFrom, dtSeconds, fastTurn = true)
            approachSpeed(StartleTimeline.speedFactorAt(phase) * maxSpeed, maxSpeed * 6f * dtSeconds)
        }
    }
}

/** 以限加速度逼近目标速度（不降到负值）。 */
private fun FishSwimRuntime.approachSpeed(desired: Float, maxDelta: Float) {
    val delta = desired - navSpeed
    navSpeed = (navSpeed + delta.coerceIn(-maxDelta, maxDelta)).coerceAtLeast(0f)
}
