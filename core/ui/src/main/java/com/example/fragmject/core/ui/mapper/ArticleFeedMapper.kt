package com.example.fragmject.core.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.ui.components.FeedCardUIState
import com.example.fragmject.core.ui.components.FooterBadge
import com.example.fragmject.core.ui.utils.AvatarUtils
import com.example.fragmject.core.ui.utils.fromHtml

/**
 * 记住 [Article] 到 [FeedCardUIState] 的映射结果：按完整文章与收藏覆盖态缓存。
 *
 * 收敛 home / search / user / system / project / collection 六处重复的
 * `remember(item, overrides[item.id]) { item.toFeedCardUIState(overrides[item.id]) }`，
 * 使映射的缓存键与取值逻辑集中一处（改映射只需改本文件与 [toFeedCardUIState]）。
 */
@Composable
fun rememberArticleFeedData(
    item: Article,
    overrides: Map<String, Boolean>,
): FeedCardUIState = remember(item, overrides[item.id]) {
    item.toFeedCardUIState(overrides[item.id])
}

/**
 * 将 [Article] 领域对象映射为通用 [FeedCardUIState]。
 *
 * 文章业务语义在此处翻译为通用字段：
 * 章节 → footerText/footerId，新/置顶 → footerBadges，收藏 → selected。
 */
fun Article.toFeedCardUIState(selectedOverride: Boolean? = null): FeedCardUIState = FeedCardUIState(
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
    selected = selectedOverride ?: collect,
    displayName = "$author$shareUser".ifBlank { "匿名" },
    avatarResId = AvatarUtils.avatarResIdBySeed("$author$shareUser".ifBlank { userId }),
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
