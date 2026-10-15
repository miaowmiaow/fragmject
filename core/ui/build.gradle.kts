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
    api(project(":core:designsystem"))
    // 公开签名里出现了 model/domain 的类型（ArticleFeedMapper 的 Article、
    // CollectInteraction 的 CollectState），因此用 api 透传，与 core:domain 的
    // `api(core:model)` 策略一致，避免消费方靠传递路径编译。
    api(project(":core:model"))
    api(project(":core:domain"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.paging.compose)
    testImplementation(libs.junit)
}