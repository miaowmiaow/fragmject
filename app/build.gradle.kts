import com.example.fragmject.convention.loadConfigProperties
import java.util.Properties

plugins {
    id("fragmject.android.application")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
    // consumer 侧：提供 :app:generateBaselineProfile，并把生成的 profile 落到
    // app/src/<variant>/generated/baselineProfiles，由 AGP 在打包时编译进 APK
    alias(libs.plugins.androidx.baselineprofile)
}

val configProperties = loadConfigProperties()

val keystoreFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.fragmject.app"

    // ========================================================================
    // 安全防护 native 层：编译 app/src/main/cpp 下的 libfragmject_security.so。
    //
    // 该 so 负责：ptrace 占坑、TracerPid、匿名可执行段统计、libc .text CRC32、
    // Frida maps 特征（编译期 XOR 混淆）、native SIGKILL。
    //
    // 为什么要显式指定 ndkVersion：
    // - AGP 有默认 NDK 版本，若与本地安装版本不一致会触发额外下载或直接失败；
    // - 本工程在 NDK 28 上验证通过（该版本 <elf.h> 未提供 ElfW，已在 security.cpp 内自行补齐）；
    // - CI / 协作开发时必须与此处保持一致，否则 native 编译结果不可复现。
    // 升级 SDK/NDK 后，同步修改此处与 CMakeLists 的 cmake_minimum_required。
    //
    // 注意：native 只为 buildTypes 中 abiFilters 声明的 ABI 编译
    // （当前 release 为 arm64-v8a，debug 额外包含 x86）。
    // ========================================================================
    ndkVersion = "28.2.13676358"
    externalNativeBuild {
        cmake {
            // 指向独立的小型 CMake 工程，只产出一个 so；
            // 不把安全代码混进其它模块，便于单独审计与按需裁剪。
            path = file("src/main/cpp/CMakeLists.txt")
            // 与 CMakeLists 的 cmake_minimum_required 保持一致（本机安装 3.22.1）。
            version = "3.22.1"
        }
    }

    defaultConfig {
        applicationId = configProperties.getProperty("applicationId")
        versionCode = configProperties.getProperty("versionCode").toInt()
        versionName = configProperties.getProperty("versionName")
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        // 单渠道无 flavor/dimension：manifest 占位符直接收敛到 defaultConfig
        manifestPlaceholders["app_channel_value"] = "free"
        manifestPlaceholders["app_name_value"] = "玩Android"
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
