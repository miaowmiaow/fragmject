import java.io.FileInputStream
import java.util.Properties

plugins {
    id("fragmject.android.library")
    id("fragmject.android.hilt")
}

val configProperties = Properties()
configProperties.load(FileInputStream(rootProject.file("config.properties")))

val baseUrl: String = configProperties.getProperty("baseUrl")

android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:android-platform"))
    implementation(project(":core:data-contract"))
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.gson)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    testImplementation(libs.junit)
}
