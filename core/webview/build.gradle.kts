plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    // WebViewManager 使用 okio.ByteString.encodeUtf8，通过 okhttp 传递引入
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines)

    // WebViewContainer 使用 rememberLauncherForActivityResult 申请权限
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
