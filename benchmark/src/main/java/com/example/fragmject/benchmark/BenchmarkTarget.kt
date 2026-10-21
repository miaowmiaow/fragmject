package com.example.fragmject.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope

/**
 * 基准参数与被测应用的通用手势。
 *
 * 被测包名不在此处声明：由 `build.gradle.kts` 从根目录 `config.properties` 的
 * `applicationId` 生成 `BuildConfig.APP_ID`，改包名时只需改一处。
 */

/** 启动后等待首屏出现的上限（毫秒）。 */
internal const val LAUNCH_TIMEOUT_MS = 5_000L

/** 采样次数：Macrobenchmark 取中位数，5 次足够稳定且不至于让单次采集过久。 */
internal const val ITERATIONS = 5

/** 每次采集连续滑动的屏数（覆盖列表条目组合、图片解码与回收复用）。 */
internal const val SWIPES_PER_ITERATION = 3

/**
 * 从屏幕下部向上滑动约半屏，模拟用户浏览列表。
 *
 * 不依赖具体控件 id：首页列表在登录前后结构不同，按坐标滑动对两种状态都成立。
 */
internal fun MacrobenchmarkScope.swipeUp(steps: Int = 20) {
    val centerX = device.displayWidth / 2
    val startY = (device.displayHeight * 0.75f).toInt()
    val endY = (device.displayHeight * 0.25f).toInt()
    device.swipe(centerX, startY, centerX, endY, steps)
}
