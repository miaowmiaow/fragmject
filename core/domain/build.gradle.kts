plugins {
    id("fragmject.android.library")
}

dependencies {
    implementation(project(":core:model"))
    // 领域层唯一刻意接受的第三方依赖：paging-common 为纯 JVM 库，PagingData 是不可变分页载体，
    // 不违反「领域层不依赖 Android framework」的硬规则；禁止引入含 Android 的 paging-runtime。
    api(libs.androidx.paging.common)
    // UseCase 构造注入仅需 JSR-330 的 @Inject 注解，由 app 组合根的 Hilt 聚合生成 Factory。
    // 领域层在构建层面不依赖 hilt-android，彻底脱离 Android framework。
    implementation(libs.javax.inject)
    implementation(libs.kotlin.stdlib)
    // 协程仅需纯 JVM 的 core 版（Flow/async/coroutineScope 等），不含 Dispatchers.Main 等 Android 调度器，
    // 与 hilt/paging 一并保证领域层构建依赖彻底脱离 Android framework。
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}