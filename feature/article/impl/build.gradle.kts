plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.feature")
    id("fragmject.android.hilt")
}

dependencies {
    api(project(":feature:article:api"))
    // 过渡依赖：WebScreen 引用 BrowseHistoryNavKey（user/api）
    implementation(project(":feature:user:api"))

    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:player"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    // WebViewManager 使用 okio.ByteString.encodeUtf8，通过 okhttp 传递引入
    implementation(libs.okhttp)

    testImplementation(libs.junit)
}