plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.kotlin.parcelize")
    id("fragmject.android.hilt")
}

dependencies {
    // Picture API (NavKeys)
    api(project(":feature:picture:api"))

    // Core
    // 说明：依赖 :core:data-repository 仅为取得 BitmapImageHandle（ImageHandle 的平台实现）。
    // 画布持有真实 android.graphics.Bitmap，必须包装成领域句柄才能交给 MediaEditor；
    // 除此之外本模块不引用 data 层任何类型，由架构测试规则 12 强制约束。
    implementation(project(":core:data-repository"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:navigation-runtime"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // 显式声明：使用 androidx.core.graphics.* / androidx.core.net.toUri /
    // androidx.core.content.ContextCompat（core-ktx）
    implementation(libs.androidx.core.ktx)
    // hiltViewModel()：接入 entry 级 ViewModelStore 后，ViewModel 必须经 Hilt 工厂创建
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}
