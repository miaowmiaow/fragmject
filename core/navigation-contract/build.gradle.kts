plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose-runtime")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    // 语义 Navigator 的 Compose 访问入口（CompositionLocal）由 compose-runtime 约定插件提供
    // 契约接口公开暴露 StateFlow（选图结果），需以 api 透传给实现方与消费方
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
