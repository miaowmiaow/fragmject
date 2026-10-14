plugins {
    id("fragmject.android.library")
    id("fragmject.kotlin.parcelize")
    id("fragmject.android.room")
    id("fragmject.android.hilt")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:data-contract"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(libs.gson)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.room3.runtime)
    testImplementation(libs.junit)

    // 迁移测试：Room Gradle 插件会把 schemas/ 自动加入 androidTest assets
    androidTestImplementation(libs.androidx.room3.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
}
