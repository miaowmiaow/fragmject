package com.example.fragmject.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Hilt 约定插件（id = "fragmject.android.hilt"）。
 *
 * 自动完成：
 * - 应用 com.google.dagger.hilt.android + com.google.devtools.ksp
 * - 添加 hilt-android + hilt-android-compiler(ksp)
 * - 版本号统一从 libs.versions.toml 读取
 */
class FragmjectAndroidHiltPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.dagger.hilt.android")
            pluginManager.apply("com.google.devtools.ksp")

            dependencies.apply {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-android-compiler").get())
            }
        }
    }
}
