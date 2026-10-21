package com.example.fragmject.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冷/温启动耗时基准。
 *
 * 运行（需设备或模拟器）：
 * `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`
 * 结果落盘在 `benchmark/build/outputs/benchmarkResults/`，同时打印到 logcat。
 * 变体名由 androidx.baselineprofile 插件创建（benchmarkRelease / nonMinifiedRelease）。
 *
 * 指标说明：[StartupTimingMetric] 给出 timeToInitialDisplay / timeToFullDisplay，
 * 前者是首帧，后者是「用户认为已经可用」的时刻，两者差距反映首屏数据装配的成本。
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /** 冷启动：进程不存在，包含 Application 初始化、Hilt 注入、首屏组合。 */
    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = BuildConfig.APP_ID,
        metrics = listOf(StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }

    /** 温启动：进程存活、Activity 重建，衡量从后台回到前台的恢复速度。 */
    @Test
    fun warmStartup() = benchmarkRule.measureRepeated(
        packageName = BuildConfig.APP_ID,
        metrics = listOf(StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }
}
