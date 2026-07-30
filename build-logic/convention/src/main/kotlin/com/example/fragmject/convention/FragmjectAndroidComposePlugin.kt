package com.example.fragmject.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Compose 约定插件（id = "fragmject.android.compose"）。
 *
 * 自动完成：
 * - 应用 org.jetbrains.kotlin.plugin.compose
 * - buildFeatures { compose = true }
 * - 添加 compose BOM + material3 + icons-extended + tooling + lifecycle-runtime-compose
 * - 版本号统一从 libs.versions.toml 读取
 */
class FragmjectAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            pluginManager.withPlugin("com.android.library") {
                configure<LibraryExtension> { buildFeatures { compose = true } }
            }
            pluginManager.withPlugin("com.android.application") {
                configure<ApplicationExtension> { buildFeatures { compose = true } }
            }

            dependencies.apply {
                val bom = libs.findLibrary("androidx-compose-bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-material3-window-size").get())
                add("implementation", libs.findLibrary("androidx-compose-material-icon-extended").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            }
        }
    }
}
