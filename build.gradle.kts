// Top-level build file. Convention plugins (build-logic) handle most subproject configuration.
// These apply-false declarations are required for the version catalog to resolve plugin versions.
plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.fragmject.android.stability.check)
    alias(libs.plugins.fragmject.android.dependency.guard)
}

// 将架构守卫任务挂载到 check，确保本地/CI 全量构建时强制校验依赖方向与稳定性配置。
// 注：verifyModuleDependencies 声明了 notCompatibleWithConfigurationCache，
// 挂载后会使 check 与配置缓存不兼容；如遇缓存问题可改为在 CI 单独触发。
tasks.matching { it.name == "check" }.configureEach {
    dependsOn("verifyModuleDependencies", "verifyComposeStabilityConfig")
}