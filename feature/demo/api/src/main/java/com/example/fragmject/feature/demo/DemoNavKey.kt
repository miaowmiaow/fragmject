package com.example.fragmject.feature.demo

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Demo Feature — 组件演示域路由表。
 *
 * 单一入口 [DemoNavKey]，内部通过 Drawer 切换所有组件示例。
 * 演示页是全屏页，**故意不**实现 `DetailPaneNavKey`（大屏不做详情面板分栏）。
 */
@Serializable
object DemoNavKey : NavKey