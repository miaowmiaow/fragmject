package com.example.fragmject.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Kotlin Serialization 能力插件（id = "fragmject.kotlin.serialization"）。
 *
 * 应用 org.jetbrains.kotlin.plugin.serialization，为使用 @Serializable 的模块
 * （如 feature api 的 NavKey typed routes）提供 serializer 代码生成能力，
 * 与「模块角色」解耦。
 */
class FragmjectKotlinSerializationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    }
}
