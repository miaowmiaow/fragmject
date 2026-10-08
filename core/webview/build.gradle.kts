plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

dependencies {
    implementation(project(":core:android-platform"))
    // 资源缓存复用领域下载端口（DownloadRepository），由 data 层基于 OkHttp 实现
    implementation(project(":core:domain"))

    implementation(libs.kotlinx.coroutines)

    // 显式声明：使用 androidx.core.net.toUri（core-ktx）。
    // core:android-platform 的 core-ktx 是 implementation，不会传递过来。
    implementation(libs.androidx.core.ktx)

    // WebViewContainer 使用 rememberLauncherForActivityResult 申请权限
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
