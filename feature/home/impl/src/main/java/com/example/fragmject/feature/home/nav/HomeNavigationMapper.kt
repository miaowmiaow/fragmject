package com.example.fragmject.feature.home.nav

import androidx.navigation3.runtime.NavKey
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.user.UserNavKey

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
 * 将导航回调转换为语义动作，集中创建 WebNavKey / UserNavKey / SystemNavKey。
 */
fun homeNavActions(onNavigate: (NavKey) -> Unit): HomeNavActions = HomeNavActions(
    onArticleClick = { onNavigate(WebNavKey(it)) },
    onAuthorClick = { onNavigate(UserNavKey(it)) },
    onChapterClick = { onNavigate(SystemNavKey(it)) },
)
