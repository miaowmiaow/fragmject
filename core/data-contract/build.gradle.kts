plugins {
    id("fragmject.android.library")
}

// 模块名含连字符，defaultNamespace 已将其归一化为 `core.data.contract`，
// 与代码包名 `com.example.fragmject.core.data.contract` 保持一致，无需显式声明。

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.junit)
}
