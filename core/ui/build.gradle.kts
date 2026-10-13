plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:model"))
    // 收藏交互封装需引用 domain 的 CollectState 契约；domain 为零 Android 依赖的纯契约层
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.paging.compose)
    testImplementation(libs.junit)
}