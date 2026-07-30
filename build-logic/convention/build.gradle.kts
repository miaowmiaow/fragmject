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
        register("androidFeature") {
            id = libs.plugins.fragmject.android.feature.get().pluginId
            implementationClass = "com.example.fragmject.convention.FragmjectAndroidFeaturePlugin"
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
    }
}
