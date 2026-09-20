plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose")
    id("fragmject.android.feature")
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
    api(project(":feature:home:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:article:api"))
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:demo:api"))
    implementation(project(":feature:search:api"))

    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}