package com.example.fragmject.core.navigation.contracts

/**
 * 收藏/分享域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（home 等）依赖本接口
 * 而非 collection feature 的 NavKey。
 */
interface CollectionNavigator {

    /** 打开我的收藏。 */
    fun openMyCollect()

    /** 打开我的分享。 */
    fun openMyShare()

    /** 打开新建分享。 */
    fun openShareArticle()
}
