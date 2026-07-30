package com.example.fragmject.core.common

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 全局深色模式状态，跨模块共享。
 *
 * 原置于 :app 内的 WanHelper 中，现提升至 core:common，
 * 使 core:ui (WanColors/WanTheme) 及 feature 模块可独立引用，
 * 无需反向依赖 :app。
 *
 * 使用方式：
 * - [com.example.fragment.project.utils.WanHelper] 负责持久化读写；
 * - [com.example.fragment.project.WanTheme] 的 Composable 收集本 StateFlow；
 * - Feature 模块可通过 ViewModel 注入本 StateFlow 来控制 UI。
 */
object DarkThemeState {
    val isDark = MutableStateFlow(false)
}
