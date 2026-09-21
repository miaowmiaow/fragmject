package com.example.fragmject.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * 详情面板渲染器：按 NavKey 渲染右侧面板内容。
 * 参数依次为：目标 NavKey、面板内导航、清除面板。
 */
typealias DetailContentRenderer = @Composable (NavKey, (NavKey) -> Unit, () -> Unit) -> Unit

/**
 * Expanded 双栏「详情面板」的运行时配置。
 *
 * 这些状态由 app 组合根在 Expanded 模式下产生，本应作为显式参数传给 Home 的
 * `registerHomeNavContents`。但阶段 2 引入 [NavContentContributor] 自主注册后，
 * 注册渲染器不再经过显式函数调用，运行时状态改由 CompositionLocal 传递：
 * Home 的 Contributor 在 `register<MainNavKey>` 的 Composable lambda 内读取，
 * 避免「全屏 vs 面板」状态跨层显式注入。
 *
 * 使用 [androidx.compose.runtime.staticCompositionLocalOf]：值为稳定引用、内容不变，
 * 无需触发订阅方重组。
 */

/** 当前选中的详情 NavKey；null 表示无详情面板。 */
val LocalSelectedDetailKey = staticCompositionLocalOf<NavKey?> { null }

/** 清除详情面板（Back / 关闭）的回调。 */
val LocalOnClearDetail = staticCompositionLocalOf<() -> Unit> { {} }

/** 详情面板渲染器。 */
val LocalDetailContent = staticCompositionLocalOf<DetailContentRenderer> { { _, _, _ -> } }
