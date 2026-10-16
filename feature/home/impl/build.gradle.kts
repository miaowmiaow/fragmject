plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

android {
    testOptions {
        // ViewModel 内调用 android.util.Log 时，JVM 单测默认会抛 "Method not mocked"；
        // 让 android.jar 的 mock 方法返回默认值（Log 为无副作用日志，可安全忽略）。
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // 组合根 :app 显式声明 :feature:home:api（见 dependencies-policy.md §6.5）。
    // 此处用 implementation 而非 api()：否则 api 层会经 impl 传递进入 app，
    // 使 APP_ALLOWED_DEPENDENCIES 对契约层空转（守卫只看直接边）。
    implementation(project(":feature:home:api"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}