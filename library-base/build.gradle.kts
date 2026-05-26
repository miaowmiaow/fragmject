import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

val configProperties = Properties()
configProperties.load(FileInputStream(rootProject.file("config.properties")))

android {
    namespace = "com.example.miaow.base"
    compileSdk {
        version = release(configProperties.getProperty("compileSdkVersion").toInt())
    }

    defaultConfig {
        minSdk = configProperties.getProperty("minSdkVersion").toInt()
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    room3 {
        schemaDirectory("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
        // 启用 BuildConfig，便于在网络层等基础组件中区分 Debug / Release
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    // 以下为被上层模块（app / library-picture）直接 import 的依赖，保留 api 以传递依赖；
    // 其余仅 library-base 内部使用的项下面走 implementation，避免编译 classpath 过度污染、提升增量编译速度。
    api(libs.androidx.core.ktx)
    // BaseDialog/PermissionsHelper 等公开类签名中暴露了 Fragment/FragmentManager，
    // 调用方必须能看见这些类型，因此 fragment.ktx 保留 api 传递。
    api(libs.androidx.fragment.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    // BaseAdapter 继承 RecyclerView.Adapter、ViewBindHolder 继承 RecyclerView.ViewHolder，
    // 上层（app）使用 holder.itemView 等成员时，编译器需要看到 RecyclerView 字节码，必须以 api 传递。
    api(libs.androidx.recyclerview)
    api(libs.coil)
    api(libs.coil.gif)
    api(libs.coil.svg)
    api(libs.coil.video)
    api(libs.gson)
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutines)
    // 仅 library-base 内部使用，不需要传递给上层：
    // material：library-base 自身不依赖 com.google.android.material；library-picture 已自行显式声明。
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}