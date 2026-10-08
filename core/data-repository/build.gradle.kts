plugins {
    id("fragmject.android.library")
    id("fragmject.android.hilt")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:android-platform"))
    implementation(project(":core:data-contract"))
    implementation(project(":core:model"))
    implementation(libs.androidx.paging.common)
    // 显式声明：MediaEditorImpl 使用 androidx.core.net.toUri（core-ktx）。
    // core:android-platform 也依赖 core-ktx，但它是 implementation，不向本模块传递。
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
