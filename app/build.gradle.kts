import com.example.fragmject.convention.loadConfigProperties
import java.util.Properties

plugins {
    id("fragmject.android.application")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

val configProperties = loadConfigProperties()

val keystoreFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.fragmject.app"

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

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}
