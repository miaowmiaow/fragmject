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

/**
 * 采样次数：Macrobenchmark 取中位数。
 *
 * ## 为什么不是 5 次
 *
 * 真机（PGEM10 / ColorOS）上厂商进程冻结器 OplusHansManager 会把测试进程冻结，规律是
 * 「进入可冻状态后 R stay=48s → M enter → M stay=3s → F enter(freeze)」（详见
 * [StartupBenchmark] 的 KDoc）。**单次采集必须在这 48 秒内跑完**，5 轮（编译 + 5 次迭代）
 * 必然超窗被冻，实测 cold 5 轮在 52 秒处冻结。
 *
 * 系统设置里的自启动 / 关联启动 / 完全允许后台行为**管不到 Hans**（实测照样冻结），
 * 且任何安装（含覆盖安装）都会重置这些开关。
 *
 * 因此这里固定 2 次，并配合「**每个用例单独一次 `am instrument` 调用**」：
 * 单次 15~20 秒，稳稳落在窗口内，不需要额外授权也能跑完。
 * 换到不会冻结的设备/模拟器后，可恢复到 5 次以获得更稳的中位数。
 */
internal const val ITERATIONS = 2

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
