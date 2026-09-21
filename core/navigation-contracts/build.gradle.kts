plugins {
    id("fragmject.android.library")
}

// 模块名含连字符，defaultNamespace 已将其归一化为 `core.navigation.contracts`，
// 与代码包名 `com.example.fragmject.core.navigation.contracts` 保持一致，无需显式声明。

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.kotlin.stdlib)
    // 语义 Navigator 的 Compose 访问入口（CompositionLocal）需要 compose-runtime
    implementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit)
}
