plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
}

dependencies {
    // 相机与条码识别：CameraX + ML Kit（feature 不得直接依赖这些 SDK，
    // 统一由本引擎模块持有，避免各 feature 各自引入一份）
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit)
    implementation(libs.barcode.scanning)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlin.stdlib)
}