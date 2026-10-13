plugins {
    id("fragmject.android.library")
    id("fragmject.android.compose-runtime")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    // 语义 Navigator 的 Compose 访问入口（CompositionLocal）由 compose-runtime 约定插件提供
    // 契约接口公开暴露 StateFlow（选图结果），需以 api 透传给实现方与消费方
    api(libs.kotlinx.coroutines.core)
    // DetailPaneNavKey / RequiresAuth 标记接口以 NavKey 为父类型，需以 api 透传，
    // 使 feature:*:api 实现标记接口时无需自行补充 navigation3 依赖
    api(libs.androidx.navigation3.runtime)
    // Navigators 聚合体用 @Inject constructor 供 Hilt 构造（替代 app 组合根 Holder）
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
}
