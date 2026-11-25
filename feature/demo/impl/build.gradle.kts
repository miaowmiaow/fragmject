plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

android {
    testOptions {
        // CalendarViewModel 在落盘失败分支调用 android.util.Log.e，JVM 单测默认会抛
        // "Method not mocked"；让 android.jar 的 mock 方法返回默认值（与 feature:home:impl 一致）。
        unitTests.isReturnDefaultValues = true
        // 日历的视觉基线：Robolectric 渲染 Compose 需要 android 资源
        unitTests.isIncludeAndroidResources = true
        // 当前只出图不入基线对比（与 core:ui 的截图测试一致）
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

dependencies {
    // 组合根 :app 显式声明 :feature:demo:api（见 dependencies-policy.md §6.5）。
    // 此处用 implementation 而非 api()：否则 api 层会经 impl 传递进入 app，
    // 使 APP_ALLOWED_DEPENDENCIES 对契约层空转（守卫只看直接边）。
    implementation(project(":feature:demo:api"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:webview"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.paging.compose)
    // 显式声明：使用 androidx.core.content.ContextCompat / androidx.core.view.*（core-ktx）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Coil
    implementation(libs.coil.compose)

    // Camera / Barcode
    implementation(project(":core:qrscanner"))

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    // 视觉基线：Roborazzi + Robolectric 在 JVM 上渲染 Compose（见 DayContentScreenshotTest）
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}