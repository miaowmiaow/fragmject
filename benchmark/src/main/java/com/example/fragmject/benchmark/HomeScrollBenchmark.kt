package com.example.fragmject.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 首页列表滚动帧耗时基准。
 *
 * 运行：`./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`
 *
 * [FrameTimingMetric] 只统计 measure 块内的帧：启动放在 setup 里，
 * 因此这里测到的是「滑动列表本身」的成本，不与冷启动混在一起。
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollHome() = benchmarkRule.measureRepeated(
        packageName = BuildConfig.APP_ID,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        setupBlock = {
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg(BuildConfig.APP_ID)), LAUNCH_TIMEOUT_MS)
            device.waitForIdle()
        },
    ) {
        repeat(SWIPES_PER_ITERATION) { swipeUp() }
    }
}
