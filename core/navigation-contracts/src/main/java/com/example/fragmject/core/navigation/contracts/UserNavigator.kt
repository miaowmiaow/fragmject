package com.example.fragmject.core.navigation.contracts

/**
 * 用户域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（home/collection/search/article 等）
 * 依赖本接口而非目标 feature 的 NavKey。
 */
interface UserNavigator {

    /** 查看用户主页。 */
    fun openUserProfile(userId: String)

    /** 打开系统设置。 */
    fun openSetting()

    /** 打开我的积分/金币。 */
    fun openMyCoin()

    /** 打开浏览历史。 */
    fun openBrowseHistory()
}
