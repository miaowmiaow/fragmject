package com.example.fragmject.core.navigation.contracts

/**
 * 首页域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（auth 等）依赖本接口
 * 而非 home feature 的 NavKey。典型场景：登录/注册成功后回首页。
 */
interface HomeNavigator {

    /** 回到首页（Tab 容器根路由）。 */
    fun openMain()

    /** 打开体系分类文章列表页。 */
    fun openSystem(cid: String)
}
