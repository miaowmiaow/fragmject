package com.example.fragmject.core.navigation.contracts

/**
 * Demo 域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（home 等）依赖本接口
 * 而非 demo feature 的 NavKey。
 */
interface DemoNavigator {

    /** 打开组件演示页。 */
    fun openDemo()
}
