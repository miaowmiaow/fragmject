plugins {
    id("com.android.library")
}

/**
 * 运行期安全防护库（从 `:app` 搬出，供其他项目复用）。
 *
 * ============================ 为什么是顶层模块 ============================
 * 它不再属于某个应用的内部实现，而是要作为「一份能力」交付给其他项目，
 * 满足 `dependencies-policy.md` 的「物理隔离 / 独立交付」门槛；
 * 因此不放进 `:core`（core 天然依赖本项目上下文）。
 *
 * ============================ 对外契约 ============================
 * - 入口：`SecurityGuard.install(context, SecurityConfig(...))`（宿主在 attachBaseContext 调用）；
 * - 配置：`SecurityConfig` / `SecurityHit` / `Severity` / `Action`；
 * - native：随 AAR 分发 `libappsecurity.so`，JNI 走动态注册，宿主无需写任何 keep 规则
 *   （规则在 `consumer-rules.pro` 里，随 AAR 自动合并）。
 */
android {
    namespace = "com.example.security"
    compileSdk = 37

    // 与 :app 保持一致：升级 NDK 时两处一起改（app 侧只为打包，native 编译在这里）
    ndkVersion = "28.2.13676358"

    defaultConfig {
        // InMemoryDexClassLoader 需要 API 26+
        minSdk = 26
        // 宿主 R8 会重命名 JNI 类与公开 API，规则必须由库自带
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        // 库不需要 BuildConfig：所有决策由宿主的 SecurityConfig 注入
        buildConfig = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // 库只依赖 Android 框架；检测项通过 SecurityFacts 抽象，可在纯 JVM 上测试
    testImplementation(libs.junit)
}
