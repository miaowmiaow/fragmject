package com.example.fragmject.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 生成项目自有的 Baseline Profile。
 *
 * 运行（需 API 33+ 的设备/模拟器，或 API 28+ 的 root 设备）：
 * `./gradlew :app:generateBaselineProfile`
 * 产物由插件复制到 `app/src/<variant>/generated/baselineProfiles/baseline-prof.txt`，
 * 后续 `:app:assembleRelease` 会自动把它编译进 APK。
 *
 * 覆盖的关键路径 = 冷启动 + 首页列表滚动。profile 只对走到的路径做 AOT 编译，
 * 因此这里刻意只保留「用户一进来就会做」的动作，避免把 profile 撑大后收益被摊薄。
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        // includeInStartupProfile：除了 main profile，再产出一份 startup profile（API 29+ 安装期
        // 单独用于冷启动的规则集）。不开启时插件会告警「No startup profile rules were generated」。
        baselineProfileRule.collect(
            packageName = BuildConfig.APP_ID,
            maxIterations = 15,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg(BuildConfig.APP_ID)), LAUNCH_TIMEOUT_MS)
            device.waitForIdle()
            repeat(SWIPES_PER_ITERATION) { swipeUp() }
            device.waitForIdle()
        }
    }
}
