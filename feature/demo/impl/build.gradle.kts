plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.feature")
    id("fragmject.android.hilt")
}

dependencies {
    api(project(":feature:demo:api"))
    // 跨域：BarcodeScanningScreen/PictureSelectorDemoScreen 引用 PictureSelectorNavKey（picture/api）
    implementation(project(":feature:picture:api"))

    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:player"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:webview"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.hilt.navigation.compose)

    // Coil
    implementation(libs.coil.compose)

    // Camera / Barcode
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit)
    implementation(libs.barcode.scanning)

    // Media3
    implementation(libs.androidx.media3.exoplayer)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}