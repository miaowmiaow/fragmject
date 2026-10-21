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
        // Robolectric 需要合并后的资源与 manifest 才能启动 Application；
        // 只有相册/媒体两个平台耦合实现使用（T-07）。
        unitTests.isIncludeAndroidResources = true
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
    // 平台耦合实现（AlbumRepositoryImpl / MediaRepositoryImpl）需要真实 ContentResolver 语义
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
