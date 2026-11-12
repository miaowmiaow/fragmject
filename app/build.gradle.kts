import com.example.fragmject.convention.loadConfigProperties
import java.util.Properties


plugins {
    id("fragmject.android.application")
    // 安全库宿主配置：构建期注入 BuildConfig.EXPECTED_SIG_SHA256（不再手写 40 行）
    id("fragmject.security.config")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
    // consumer 侧：提供 :app:generateBaselineProfile，并把生成的 profile 落到
    // app/src/<variant>/generated/baselineProfiles，由 AGP 在打包时编译进 APK
    alias(libs.plugins.androidx.baselineprofile)
}

val configProperties = loadConfigProperties()

/**
 * 矩阵测试用的 ABI 覆盖（`-PmatrixAbi=x86_64`）。
 *
 * 默认只出 arm64-v8a：本机是 Apple Silicon，跑不了 x86 镜像；而 GitHub Actions 的 runner 是 x86，
 * 只能跑 x86_64 模拟器。CI 的版本矩阵因此用这个参数单独出一份 x86_64 的壳包，
 * 真实发布产物（不带该参数）不受影响。
 */
val matrixAbi: String? = providers.gradleProperty("matrixAbi").orNull

/**
 * DEX 壳开关（H6/S1）：打开后 manifest 的 android:name 指向壳类，
 * 同时 build-logic 会注册 packShellRelease 把产物打成壳。
 * 两者必须成对出现，否则会得到「manifest 指向不存在的类」的包。
 */
val dexShellEnabled = providers.gradleProperty("enableDexShell").map(String::toBoolean).getOrElse(false)

val keystoreFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}


android {
    namespace = "com.example.fragmject.app"

    // 安全防护的 native（libappsecurity.so）随 :security 模块编译，app 只负责打包与 ABI 过滤，
    // 因此这里不再有 ndkVersion / externalNativeBuild（升级 NDK 请改 :security/build.gradle.kts）。

    defaultConfig {
        applicationId = configProperties.getProperty("applicationId")
        versionCode = configProperties.getProperty("versionCode").toInt()
        versionName = configProperties.getProperty("versionName")
        ndk {
            //noinspection ChromeOsAbiSupport
            if (matrixAbi != null) {
                // 矩阵构建：只带指定 ABI（CI 用 x86_64）
                abiFilters += matrixAbi
            } else {
                abiFilters += "arm64-v8a"
            }
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        // 单渠道无 flavor/dimension：manifest 占位符直接收敛到 defaultConfig
        manifestPlaceholders["app_channel_value"] = "free"
        manifestPlaceholders["app_name_value"] = "玩Android"
        // Application 入口：默认是真实实现；开壳时替换为壳类（壳类由 shell dex 提供）
        manifestPlaceholders["applicationName"] = if (dexShellEnabled) {
            "com.example.shell.StubApplication"
        } else {
            "com.example.fragmject.app.FragmjectApplication"
        }
        // 矩阵测试专用：打开 PLT/GOT 检测（默认关，见 SecurityCheck.PLT_HOOK 的说明）。
        // 只在 -PenablePltCheck=true 时为 true，正式产物恒为 false。
        buildConfigField(
            "boolean",
            "ENABLE_PLT_CHECK",
            providers.gradleProperty("enablePltCheck").map(String::toBoolean).getOrElse(false).toString(),
        )
        // 签名期望值由 `fragmject.security.config` 插件注入 BuildConfig.EXPECTED_SIG_SHA256；
        // 取不到证书时注入空串（运行期 = 不启用签名校验），构建不会失败。
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // 仓库内的 keystore.properties / keystore.key 是有意提供的【测试专用】密钥，不用于正式发布，
    // 入库属于有意为之，不视为凭据泄漏；正式发布请使用独立的私有签名并通过安全方式管理。
    // 仅当 keystore.properties 存在且配置完整时才创建 config，否则走默认 debug 签名
    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("config") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("config")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("config")
            //noinspection ChromeOsAbiSupport
            ndk.abiFilters += "x86"
        }
    }
}

dependencies {
    // 无 :core:android-platform：该模块只被 core 内部消费，app 经领域端口间接使用，
    // 依赖守卫 APP_ALLOWED_DEPENDENCIES 已不再放行此边
    // data-repository / database：app 源码零 import，仅因其中 @Module/@InstallIn 需进入
    // Hilt SingletonComponent 而声明（组合根聚合），勿删
    // 运行期安全防护库（H7 阶段二：已从 app/security 搬到顶层 :security）
    implementation(project(":security"))
    implementation(project(":core:data-repository"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:network"))
    implementation(project(":core:webview"))
    // feature:*:impl —— 组合根聚合 NavContent / Hilt 入口
    implementation(project(":feature:article:impl"))
    implementation(project(":feature:authenticator:impl"))
    implementation(project(":feature:auth:impl"))
    implementation(project(":feature:collection:impl"))
    implementation(project(":feature:demo:impl"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:search:impl"))
    implementation(project(":feature:user:impl"))
    implementation(project(":feature:picture:impl"))
    // feature:*:api —— 组合根要用各 feature 的 NavKey 装配导航图（AppNavGraph / DeepLinkMatchers /
    // AppNavigators），必须显式声明：impl 侧已降为 implementation，不再传递，删掉任一条即编译失败
    implementation(project(":feature:article:api"))
    implementation(project(":feature:authenticator:api"))
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:demo:api"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:search:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:picture:api"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // entry 级 ViewModelStore：ViewModel 随 NavEntry 出栈而清除（NavDisplay 的 entryDecorators）
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.coil.video)
    // 显式声明：FragmjectApplication 使用 okhttp3.OkHttpClient 装配 Coil ImageLoader，
    // 此前靠 coil 传递，coil 升级到不传递 okhttp 的版本会静默失败
    implementation(libs.okhttp)

    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // API 26–27 没有安装期 profile 编译，需 ProfileInstaller 在本机异步 dexopt；
    // API 28+ 由系统在安装时直接消费 APK 内的 baseline profile，此依赖不参与该路径。
    implementation(libs.androidx.profileinstaller)

    // Baseline Profile 采集入口：只在 generateBaselineProfile 时驱动 :benchmark 跑仪器测试，
    // 不进入任何运行时 / 打包产物（见 dependencies-policy 的测试模块说明）
    baselineProfile(project(":benchmark"))

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}
