package com.example.fragmject.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Compose 约定插件（id = "fragmject.android.compose"）。
 *
 * 自动完成：
 * - 应用 org.jetbrains.kotlin.plugin.compose
 * - buildFeatures { compose = true }
 * - 添加 compose BOM + runtime（必选运行时）
 * - 追加 animation + material + material3 + lifecycle-runtime-compose 等完整 UI 依赖
 * - 接入根目录 compose_stability_config.conf 稳定性配置
 * - 版本号统一从 libs.versions.toml 读取
 *
 * 若模块仅需 compose-runtime（如仅用 CompositionLocal 的契约/运行时层），
 * 请改用轻量插件 id = "fragmject.android.compose-runtime"
 * （见 [FragmjectAndroidComposeRuntimePlugin]），避免引入 material3 等多余 UI 依赖。
 */
class FragmjectAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyComposeBase()
            dependencies.apply {
                add("implementation", libs.findLibrary("androidx-compose-animation").get())
                add("implementation", libs.findLibrary("androidx-compose-material").get())
                add("implementation", libs.findLibrary("androidx-compose-material-icon-core").get())
                add("implementation", libs.findLibrary("androidx-compose-material-icon-extended").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-material3-window-size").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            }
        }
    }
}

/**
 * Compose 必选基础：应用 compose 编译器插件、开启 buildFeatures.compose、
 * 接入稳定性配置，并添加 compose BOM + runtime（必选运行时）。
 * 完整版（[FragmjectAndroidComposePlugin]）与轻量版（[FragmjectAndroidComposeRuntimePlugin]）共用。
 */
internal fun Project.applyComposeBase() {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("compose_stability_config.conf")
        )
    }

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
        add("implementation", libs.findLibrary("androidx-compose-runtime").get())
    }
}
