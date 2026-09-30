plugins {
    id("fragmject.android.library")
}

// 模块名含连字符，defaultNamespace 已将其归一化为 `core.data.contract`，
// 与代码包名 `com.example.fragmject.core.data.contract` 保持一致，无需显式声明。

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlin.stdlib)
    // 纯契约层仅需 Flow/suspend，与 core:domain 一致只用 -core，
    // 不引入含 Android Dispatchers.Main 的 -android 版本。
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
