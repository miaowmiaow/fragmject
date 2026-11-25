plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            // Roborazzi 在 JVM 上渲染 Compose 需要 android 资源与 native graphics
            isIncludeAndroidResources = true
            // 当前阶段只「出图入库」，不接 diff：让 captureRoboImage 无条件写文件。
            // 接入基线对比时应改成由 -Proborazzi.test.record / verify 控制。
            all { it.systemProperty("roborazzi.test.record", "true") }
        }
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
    testImplementation(libs.kotlinx.coroutines.test)
    // 视觉基线：Roborazzi + Robolectric 在 JVM 上渲染 Compose，按不同字号倍率出图
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}