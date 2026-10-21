plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

dependencies {
    // 组合根 :app 显式声明 :feature:article:api（见 dependencies-policy.md §6.5）。
    // 此处用 implementation 而非 api()：否则 api 层会经 impl 传递进入 app，
    // 使 APP_ALLOWED_DEPENDENCIES 对契约层空转（守卫只看直接边）。
    implementation(project(":feature:article:api"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:webview"))

    implementation(libs.androidx.activity.compose)
    // 显式声明：使用 androidx.core.view.WindowCompat / WindowInsetsCompat（core-ktx）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}