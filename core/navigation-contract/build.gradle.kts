plugins {
    id("fragmject.android.library")
}

android {
    namespace = "com.example.fragmject.core.navigation.contract"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.kotlin.stdlib)
    // 语义 Navigator 的 Compose 访问入口（CompositionLocal）需要 compose-runtime
    implementation(libs.androidx.compose.runtime)
    // 契约接口公开暴露 StateFlow（选图结果），需以 api 透传给实现方与消费方
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
