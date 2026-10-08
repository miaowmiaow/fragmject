import java.io.FileInputStream
import java.util.Properties

plugins {
    id("fragmject.android.application")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

val configProperties = Properties()
configProperties.load(FileInputStream(rootProject.file("config.properties")))

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
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // 开源项目忽略签名相关配置的安全性问题，实际项目请使用安全的方式管理签名信息
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

    flavorDimensions += "tier"

    productFlavors {
        create("free") {
            applicationIdSuffix = ".free"
            dimension = "tier"
            manifestPlaceholders["app_channel_value"] = name
            manifestPlaceholders["app_name_value"] = "玩Android"
        }
    }
}

dependencies {
    implementation(project(":core:android-platform"))
    implementation(project(":core:data-repository"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:network"))
    implementation(project(":core:webview"))
    implementation(project(":feature:article:impl"))
    implementation(project(":feature:auth:impl"))
    implementation(project(":feature:collection:impl"))
    implementation(project(":feature:demo:impl"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:search:impl"))
    implementation(project(":feature:user:impl"))
    implementation(project(":feature:picture:impl"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.coil.video)

    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}
