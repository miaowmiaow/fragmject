package com.example.fragmject.feature.home.nav

import com.example.fragmject.core.navigation.contract.ArticleNavigator
import com.example.fragmject.core.navigation.contract.HomeNavigator
import com.example.fragmject.core.navigation.contract.UserNavigator

/**
 * Home 域语义导航动作。
 *
 * 页面层只上抛语义事件（文章链接 / 作者 userId / 章节 chapterId），
 * 不直接接触任何 NavKey；跨 feature NavKey 的创建集中在本文件（导航编排层）完成。
 */
data class HomeNavActions(
    val onArticleClick: (String) -> Unit = {},
    val onAuthorClick: (String) -> Unit = {},
    val onChapterClick: (String) -> Unit = {},
)

/**
 * 将语义导航契约转换为 Home 域语义动作。
 *
 * 跨域（文章/用户）依赖语义 Navigator；[onChapterClick] 是 Home 内部导航
 * （体系分类列表），由 [HomeNavigator] 语义提供，本模块不 import 跨 feature NavKey。
 */
fun homeNavActions(
    articleNavigator: ArticleNavigator,
    userNavigator: UserNavigator,
    homeNavigator: HomeNavigator,
): HomeNavActions = HomeNavActions(
    onArticleClick = { articleNavigator.openArticle(it) },
    onAuthorClick = { userNavigator.openUserProfile(it) },
    onChapterClick = { homeNavigator.openSystemTree(it) },
)
