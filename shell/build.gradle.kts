plugins {
    id("com.android.library")
}

/**
 * DEX 壳的「壳代码」模块（H6 / S1）。
 *
 * ============================ 约束（勿改） ============================
 * 1. **纯 Java、零依赖**：壳 dex 会被塞进 APK 作为唯一的 `classes.dex`，
 *    任何依赖（Kotlin stdlib、androidx）都会让壳体积膨胀并暴露更多符号；
 * 2. **不能被 `:app` 依赖**：壳类一旦进入业务 dex，就等于没有壳；
 *    它只以「构建产物」的形式被 `packShellRelease` 任务取用；
 * 3. `compileSdk` / `minSdk` 与 `config.properties` 保持一致（37 / 26）。
 */
android {
    namespace = "com.example.shell"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // 壳代码不需要 buildConfig / 资源，关掉以减少产物噪音
    buildFeatures {
        buildConfig = false
    }
}
