package com.example.fragmject.core.navigation.contracts

/**
 * 文章域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（collection/home/search/user 等）
 * 依赖本接口而非目标 feature 的 NavKey，从而消除跨 feature 路由耦合。
 */
interface ArticleNavigator {

    /** 打开文章详情（WebView 网页）。 */
    fun openArticle(url: String)
}
