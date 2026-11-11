import com.example.fragmject.convention.loadConfigProperties
import java.util.Properties

plugins {
    id("com.android.library")
}

/**
 * 配置来源与 `:app` 一致：根目录 `config.properties`（唯一事实来源）。
 *
 * 但本模块同时要满足「可被拷到其他工程单独构建」，所以：
 * - 读取失败（文件不存在）不抛错，退回空 Properties；
 * - 每个字段都有内置默认值，缺键时用默认值，不硬失败。
 *
 * 这样在本仓库里改 `compileSdkVersion` / `minSdkVersion` / `ndkVersion` / `cmakeVersion`
 * 一处即可同时作用于 `:app` 与 `:security`，不会再出现两边版本漂移。
 */
val buildSettings: Properties = runCatching { loadConfigProperties() }.getOrElse { Properties() }

fun configInt(key: String, fallback: Int): Int =
    buildSettings.getProperty(key)?.toIntOrNull() ?: fallback

fun configString(key: String, fallback: String): String =
    buildSettings.getProperty(key)?.takeIf { it.isNotBlank() } ?: fallback

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
    // 来自 config.properties（:app 用同一份），缺键时回退到内置默认值
    compileSdk = configInt("compileSdkVersion", 37)

    // native 编译在这里发生；版本同样取自 config.properties，与 SDK 版本一起维护
    ndkVersion = configString("ndkVersion", "28.2.13676358")

    defaultConfig {
        // InMemoryDexClassLoader 需要 API 26+（config.properties 里也是 26）
        minSdk = configInt("minSdkVersion", 26)
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
            // 与本地安装版本保持一致，覆盖在 config.properties 的 cmakeVersion
            version = configString("cmakeVersion", "3.22.1")
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
