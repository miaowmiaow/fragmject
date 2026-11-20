plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // 宠物只消费颜色令牌：AppColors。
    api(project(":core:designsystem"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
}
