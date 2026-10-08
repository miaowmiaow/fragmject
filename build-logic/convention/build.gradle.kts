import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.example.fragmject.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.androidx.room3.gradle.plugin)
    compileOnly(libs.gradle)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kotlin.compose.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.fragmject.android.application.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidApplicationPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.fragmject.android.compose.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidComposePlugin"
        }
        register("androidComposeRuntime") {
            // 直接硬编码 id：避免与 fragmject-android-compose 产生版本目录访问器前缀冲突
            // （fragmject.android.compose-runtime 是 compose 的前缀扩展，无法共存于 catalog 访问器）
            id = "fragmject.android.compose-runtime"
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidComposeRuntimePlugin"
        }
        register("kotlinParcelize") {
            id = libs.plugins.fragmject.kotlin.parcelize.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectKotlinParcelizePlugin"
        }
        register("kotlinSerialization") {
            id = libs.plugins.fragmject.kotlin.serialization.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectKotlinSerializationPlugin"
        }
        register("androidHilt") {
            id = libs.plugins.fragmject.android.hilt.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidHiltPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.fragmject.android.library.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidLibraryPlugin"
        }
        register("androidRoom") {
            id = libs.plugins.fragmject.android.room.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidRoomPlugin"
        }
        register("stabilityCheck") {
            id = libs.plugins.fragmject.android.stability.check.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidStabilityCheckPlugin"
        }
        register("dependencyGuard") {
            id = libs.plugins.fragmject.android.dependency.guard.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidDependencyGuardPlugin"
        }
    }
}
