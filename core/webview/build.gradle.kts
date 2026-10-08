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

    // WebViewContainer 使用 rememberLauncherForActivityResult 申请权限
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
