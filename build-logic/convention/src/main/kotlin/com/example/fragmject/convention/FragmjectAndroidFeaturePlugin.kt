package com.example.fragmject.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Feature 模块约定插件（id = "fragmject.android.feature"）。
 *
 * 自动应用：
 * - org.jetbrains.kotlin.plugin.parcelize
 * - org.jetbrains.kotlin.plugin.serialization
 */
class FragmjectAndroidFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.parcelize")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    }
}
