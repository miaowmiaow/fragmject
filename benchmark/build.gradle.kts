import com.example.fragmject.convention.loadConfigProperties

plugins {
    // com.android.test：只产出仪器测试 APK，不参与生产打包，也不进入 app 的运行时依赖图
    alias(libs.plugins.android.test)
    // producer 侧：把采集到的 profile 交给 app 侧的 consumer 任务合并
    alias(libs.plugins.androidx.baselineprofile)
}

val configProperties = loadConfigProperties()

android {
    namespace = "com.example.fragmject.benchmark"

    // 与本工程其它模块保持一致（config.properties 是唯一来源，勿在此硬编码）
    compileSdk = configProperties.getProperty("compileSdkVersion").toInt()

    defaultConfig {
        // Macrobenchmark 的下限是 API 28；BaselineProfileRule 在未 root 的设备上需要 API 33+
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 模拟器上 benchmark 默认以 EMULATOR 报错中止；显式放行，便于无真机时采集。
        // 真机上该参数无副作用（只对匹配到的错误生效）。
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
        // 模拟器默认会被宏基准直接 skip（AssumptionViolatedException），显式放行。
        testInstrumentationRunnerArguments["androidx.benchmark.skipOnEmulator"] = "false"
        // 被测包名：直接取 config.properties 的 applicationId，测试代码里用 BuildConfig.APP_ID，
        // 避免改包名后 benchmark 仍指向旧包而静默失败
        buildConfigField(
            "String",
            "APP_ID",
            "\"${configProperties.getProperty("applicationId")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // 被测目标：AGP 会把 :app 的 APK 一同安装到设备上
    targetProjectPath = ":app"
    // com.android.test 模块自己就是被测进程的启动者（self-instrumenting），
    // 否则 benchmark 无法在目标进程重启后继续执行
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

// 在模拟器上跑宏基准还必须覆盖规则集：nonMinifiedRelease 是 Baseline Profile 采集变体，
// 插件给它注入 `androidx.benchmark.enabledRules=baselineprofile`，MacrobenchmarkRule 的用例
// 会被 AssumptionViolatedException 直接跳过。该参数不支持逗号分隔（实测
// `baselineprofile,macrobenchmark` 无效），只能在跑基准时用命令行整体覆盖：
//   -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=macrobenchmark
// 采集 Baseline Profile 时不要加，否则 BaselineProfileGenerator 反而被跳过。
// 真机侧 AppSecurityGuard 会按模拟器特征阻断 benchmarkRelease，因此模拟器只能用这一变体。

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    // 2.4.0 起 MacrobenchmarkScope 继承 UiAutomatorTestScope，device 由此提供；
    // 显式声明版本，避免被其它路径降到不兼容的 2.3.x
    implementation(libs.androidx.test.uiautomator)
}
