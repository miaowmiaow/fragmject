plugins {
    id("fragmject.android.library")
    id("fragmject.android.hilt")
}

android {
    testOptions {
        // UserRepositoryImpl#logout 在「远端已登出但本地清理失败」分支会调用 android.util.Log.w，
        // JVM 单测默认对该类 android.jar 方法抛 "Method not mocked"。Log 仅打日志、无副作用，
        // 让 mock 方法返回默认值即可（与 feature/home/impl 的处理保持一致）。
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:android-platform"))
    implementation(project(":core:data-contract"))
    implementation(project(":core:model"))
    implementation(libs.androidx.paging.common)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
