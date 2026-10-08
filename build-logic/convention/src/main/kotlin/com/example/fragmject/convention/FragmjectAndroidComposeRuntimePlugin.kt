package com.example.fragmject.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Compose 轻量运行时约定插件（id = "fragmject.android.compose-runtime"）。
 *
 * 面向仅需 compose-runtime（如仅用 CompositionLocal 的语义 Navigator 契约/运行时层）、
 * 不需要 material3 / animation 等重 UI 依赖的模块，避免引入多余依赖。
 *
 * 自动完成（与完整版 [FragmjectAndroidComposePlugin] 共用 [applyComposeBase]）：
 * - 应用 org.jetbrains.kotlin.plugin.compose
 * - buildFeatures { compose = true }
 * - 添加 compose BOM + runtime（必选运行时）
 * - 接入 compose_stability_config.conf 稳定性配置
 */
class FragmjectAndroidComposeRuntimePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyComposeBase()
    }
}
