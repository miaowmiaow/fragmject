package com.example.fragmject.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 导航渲染回调集合。
 *
 * 与导航目标（NavKey）解耦：全屏 entry 与面板 detailContent 两条路径共用同一渲染器，
 * 但注入不同的回调语义——
 * - 全屏：[onNavigateUp] 回退 backStack；
 * - 面板：[onNavigateUp] 清除 selectedDetailKey。
 */
data class NavCallbacks(
    val onNavigate: (NavKey) -> Unit,
    val onNavigateUp: () -> Unit,
    val onPopBackStack: (NavKey) -> Unit = {},
)
