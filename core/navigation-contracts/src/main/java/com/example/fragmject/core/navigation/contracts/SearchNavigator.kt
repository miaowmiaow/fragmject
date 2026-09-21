package com.example.fragmject.core.navigation.contracts

/**
 * 搜索域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（home 等）依赖本接口
 * 而非 search feature 的 NavKey。
 */
interface SearchNavigator {

    /** 打开搜索页并携带关键字。 */
    fun openSearch(key: String)
}
