package com.example.fragmject.core.ui.components

import android.text.Html
import androidx.compose.runtime.Immutable
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleTag
import com.example.fragmject.core.ui.utils.AvatarUtils

/**
 * ArticleCard 的 UI 层展示模型。
 *
 * 将 [Article]（30+ 字段的网络/持久层领域对象）映射为仅含 UI 所需的 16 个字段的稳定 data class，
 * 避免将网络模型裸传给 Compose 组件。
 *
 * [isCollected] 为收藏状态的初始值，点击收藏后由组件内部的 [remember] 管理乐观更新。
 *
 * 标 [Immutable]：所有字段均为 val 且类型不可变（List 而非 MutableList），
 * 让 Compose 编译器直接信任其稳定性，无需依赖外部 stability_config 配置。
 */
@Immutable
data class ArticleCardUiState(
    val id: String,
    val title: String,
    val desc: String,
    val author: String,
    val shareUser: String,
    val niceDate: String,
    val userId: String,
    val link: String,
    val chapterId: String,
    val chapterName: String,
    val envelopePic: String,
    val top: Boolean,
    val fresh: Boolean,
    val isCollected: Boolean,
    val tags: List<ArticleTag>?,
    // 预计算的派生字段：避免在每次重组时重复做字符串拼接与资源 ID 查找，
    // 降低 LazyColumn item 在测量/绘制阶段的计算开销。
    val displayName: String,
    val avatarResId: Int,
)

/**
 * 将 [Article] 领域对象映射为 [ArticleCardUiState]。
 * 放在 core:ui 层（而非 core:model）以避免 core:model → core:ui 的循环依赖。
 */
fun Article.toArticleCardUiState(): ArticleCardUiState = ArticleCardUiState(
    id = id,
    title = fromHtml(title),
    desc = fromHtml(desc),
    author = author,
    shareUser = shareUser,
    niceDate = niceDate,
    userId = userId,
    link = link,
    chapterId = chapterId,
    chapterName = fromHtml(formatChapterName(superChapterName, chapterName)),
    envelopePic = envelopePic.replace("http://", "https://"),
    top = top,
    fresh = fresh,
    isCollected = collect,
    tags = tags,
    // 派生字段在此一次性计算：displayName 的字符串拼接、avatarResId 的 userId.toInt() 异常路径
    // 都不会在后续每次重组时重复执行。
    displayName = "$author$shareUser".ifBlank { "匿名" },
    avatarResId = AvatarUtils.avatarResId(userId),
)

/** HTML → 纯文本（UI 展示专用，保持在 core:ui 层，避免 core:model 依赖 android.text）。 */
private fun fromHtml(str: String): String {
    return Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
}

/** 用「·」拼接多级章节名。 */
private fun formatChapterName(vararg names: String): String {
    val stringBuilder = StringBuilder()
    for ((index, name) in names.withIndex()) {
        if (index > 0) stringBuilder.append("·")
        stringBuilder.append(name)
    }
    return stringBuilder.toString()
}