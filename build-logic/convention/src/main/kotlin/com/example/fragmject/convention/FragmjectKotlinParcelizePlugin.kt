package com.example.fragmject.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Kotlin Parcelize 能力插件（id = "fragmject.kotlin.parcelize"）。
 *
 * 应用 org.jetbrains.kotlin.plugin.parcelize，为使用 @Parcelize 的模块提供
 * Parcelable 代码生成能力，与「模块角色」解耦。
 */
class FragmjectKotlinParcelizePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.parcelize")
    }
}
