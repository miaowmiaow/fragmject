plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.kotlin.parcelize")
    id("fragmject.android.hilt")
}

dependencies {
    // Picture API (NavKeys)
    // 组合根 :app 显式声明 :feature:picture:api（见 dependencies-policy.md §6.5）。
    // 此处用 implementation 而非 api()：否则 api 层会经 impl 传递进入 app，
    // 使 APP_ALLOWED_DEPENDENCIES 对契约层空转（守卫只看直接边）。
    implementation(project(":feature:picture:api"))

    // Core
    // designsystem：UI 一律使用 AppColors 设计令牌，不得出现 Color(0xFF...) 字面量（Konsist 规则 24）
    implementation(project(":core:designsystem"))
    implementation(project(":core:image-editor"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:navigation-runtime"))

    // 显式声明：使用 androidx.activity.compose.rememberLauncherForActivityResult
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // 显式声明：PictureEditorViewModel 使用 viewModelScope
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // 显式声明：使用 androidx.core.graphics.* / androidx.core.net.toUri /
    // androidx.core.content.ContextCompat（core-ktx）
    implementation(libs.androidx.core.ktx)
    // hiltViewModel()：接入 entry 级 ViewModelStore 后，ViewModel 必须经 Hilt 工厂创建
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}
