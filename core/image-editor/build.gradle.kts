plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

dependencies {
    // domain：仅引用 MediaRepository 落盘端口（保存结果写入相册）
    implementation(project(":core:domain"))
    // android-platform：位图解码（BitmapDecoder 的 Context 扩展）
    implementation(project(":core:android-platform"))

    // androidx.core.graphics.*（createBitmap / scale / values / toColorInt 等）
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
