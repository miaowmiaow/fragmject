package com.example.fragmject.feature.search.mapper

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.ui.components.FeedCardUiState
import com.example.fragmject.core.ui.components.FooterBadge
import com.example.fragmject.core.ui.utils.AvatarUtils
import com.example.fragmject.core.ui.utils.fromHtml

/**
 * 将 [Article] 领域对象映射为通用 [FeedCardUiState]。
 */
fun Article.toFeedCardUiState(): FeedCardUiState = FeedCardUiState(
    id = id,
    title = fromHtml(title),
    desc = fromHtml(desc),
    date = niceDate,
    userId = userId,
    link = link,
    footerId = chapterId,
    footerText = fromHtml(formatChapterName(superChapterName, chapterName)),
    coverUrl = envelopePic.replace("http://", "https://"),
    footerBadges = buildList {
        if (fresh) add(FooterBadge("新"))
        if (top) add(FooterBadge("置顶"))
    },
    selected = collect,
    displayName = "$author$shareUser".ifBlank { "匿名" },
    avatarResId = AvatarUtils.avatarResId(userId),
)

/** 用「·」拼接多级章节名。 */
private fun formatChapterName(vararg names: String): String {
    val stringBuilder = StringBuilder()
    for ((index, name) in names.withIndex()) {
        if (index > 0) stringBuilder.append("·")
        stringBuilder.append(name)
    }
    return stringBuilder.toString()
}
