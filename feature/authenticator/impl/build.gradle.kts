plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

dependencies {
    api(project(":feature:authenticator:api"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))
    // 二维码导出：复用 core:webview 承载本地 H5 页面（不引入二维码生成库）
    implementation(project(":core:webview"))
    // 扫码导入：复用 core:qrscanner（CameraX + ML Kit 由该引擎独占持有）
    implementation(project(":core:qrscanner"))
    // 扫码页自己做权限判断与申请（未授权时整块换成引导卡片，避免引擎静默不绘制）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}