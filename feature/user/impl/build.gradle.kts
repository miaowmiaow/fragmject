plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.hilt")
}

android {
    testOptions {
        // SettingViewModel#logout 的异常分支会调用 android.util.Log.e；JVM 单测默认
        // 对该类 android.jar 方法抛 "Method not mocked"，Log 无副作用，返回默认值即可。
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // 组合根 :app 显式声明 :feature:user:api（见 dependencies-policy.md §6.5）。
    // 此处用 implementation 而非 api()：否则 api 层会经 impl 传递进入 app，
    // 使 APP_ALLOWED_DEPENDENCIES 对契约层空转（守卫只看直接边）。
    implementation(project(":feature:user:api"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation-runtime"))
    implementation(project(":core:navigation-contract"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}