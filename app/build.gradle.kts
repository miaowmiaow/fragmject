import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val configProperties = Properties()
configProperties.load(FileInputStream(rootProject.file("config.properties")))

val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))

android {
    namespace = "com.example.fragment.project"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = configProperties.getProperty("applicationId")
        minSdk = configProperties.getProperty("minSdkVersion").toInt()
        targetSdk = configProperties.getProperty("targetSdkVersion").toInt()
        versionCode = configProperties.getProperty("versionCode").toInt()
        versionName = configProperties.getProperty("versionName")
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    //noinspection WrongGradleMethod
    room {
        schemaDirectory("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("config") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            // 启用代码压缩、优化及混淆
            isMinifyEnabled = true
            // 启用资源压缩，需配合 minifyEnabled=true 使用
            isShrinkResources = true
            // 指定混淆保留规则
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("config")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("config")
            //noinspection ChromeOsAbiSupport
            ndk.abiFilters += "x86"
        }
    }

    flavorDimensions += "tier"

    //获取渠道信息：Context.getMetaData("app_channel")
    //创建产品风味
    productFlavors {
        create("free") {
            //应用包名添加后缀
            applicationIdSuffix = ".free"
            //关联维度
            dimension = "tier"
            manifestPlaceholders["app_channel_value"] = name
            manifestPlaceholders["app_name_value"] = "玩Android"
        }
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":library-base"))
    implementation(project(":library-picture"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icon.core)
    implementation(libs.androidx.compose.material.icon.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    // Android Studio Preview support
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // UI Tests
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    // Optional - Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.leakcanary.android)
}