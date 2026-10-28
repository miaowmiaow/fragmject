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
 *
 * ## 温启动在部分真机上不可用（已知设备限制）
 *
 * PGEM10（ColorOS）上 `warmStartup` 会"卡住"数小时，实测链路如下（logcat 证据）：
 * 1. 每轮采集都要执行 `perfetto --background-wait`，库用**无超时**的 shell 命令等它返回；
 * 2. 命令发出约 1 秒后，厂商进程冻结器
 *    `OplusHansManager: freeze uid:… com.example.fragmject.benchmark pids:[…] scene: LcdOn`
 *    把测试进程冻结（与屏幕是否点亮、迭代到第几轮无关，触发条件是"长时间无用户操作"）；
 * 3. 冻结期间线程挂起，命令输出无人读取，直到设备侧解冻才继续——实测间隔 5 小时，
 *    且 perfetto 以 `EXITCODE=2 / Some perfetto data sources may not be ready` 失败返回，
 *    该轮 trace 不可用，用例最终以空 message 失败。
 *
 * 规避（采集前执行，采完恢复）：
 * ```
 * adb shell svc power stayon true      # 常亮，避免进入空闲冻结窗口
 * adb shell dumpsys deviceidle disable # 关 doze
 * ```
 * 设备插电、不要跑过夜。温启动仍拿不到可信结果时，Home 基线以 `coldStartup` +
 * `HomeScrollBenchmark` 为准，不要为了 warm 反复重跑。
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
