package com.example.fragmject.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.fragmject.core.ui.R
import com.example.fragmject.core.designsystem.WanSpacing
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleTag
import com.example.fragmject.core.ui.AvatarHelper
import kotlinx.coroutines.launch

/**
 * ArticleCard 的 UI 层展示模型。
 *
 * 将 [Article]（30+ 字段的网络/持久层领域对象）映射为仅含 UI 所需的 16 个字段的稳定 data class，
 * 避免将网络模型裸传给 Compose 组件。
 *
 * [isCollected] 为收藏状态的初始值，点击收藏后由组件内部的 [remember] 管理乐观更新。
 */
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
) {
    val displayName: String get() = "$author$shareUser".ifBlank { "匿名" }
}

/**
 * 将 [Article] 领域对象映射为 [ArticleCardUiState]。
 * 放在 core:ui 层（而非 core:model）以避免 core:model → core:ui 的循环依赖。
 */
fun Article.toArticleCardUiState(): ArticleCardUiState = ArticleCardUiState(
    id = id,
    title = titleHtml,
    desc = descHtml,
    author = author,
    shareUser = shareUser,
    niceDate = niceDate,
    userId = userId,
    link = link,
    chapterId = chapterId,
    chapterName = chapterNameHtml,
    envelopePic = httpsEnvelopePic,
    top = top,
    fresh = fresh,
    isCollected = collect,
    tags = tags,
)

/**
 * 通用文章卡片组件——零 NavKey 依赖，所有导航/交互通过回调外传。
 *
 * @param data        卡片展示数据（[ArticleCardUiState]）
 * @param onArticleClick 点击卡片主体 → 参数为文章 link
 * @param onUserClick    点击头像 → 参数为 userId
 * @param onChapterClick 点击章节/标签区域 → 参数为 chapterId（也用于"新"/"置顶"）
 * @param onTagClick     点击分类标签按钮 → 参数为 cid
 * @param onCollectClick 点击收藏按钮 → 参数为 (articleId, 是否收藏)
 */
@Composable
fun ArticleCard(
    data: ArticleCardUiState,
    modifier: Modifier = Modifier,
    onArticleClick: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onChapterClick: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onCollectClick: suspend (String, Boolean) -> Unit = { _, _ -> },
) {
    val scope = rememberCoroutineScope()
    var collected by remember(data.id) { mutableStateOf(data.isCollected) }
    val collectResId = getCollectResId(collected)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(WanSpacing.cardCornerRadius))
            .clipToBounds()
            .clickable { onArticleClick(data.link) }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = WanSpacing.cardHorizontal, top = WanSpacing.cardHorizontal, end = WanSpacing.cardHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = AvatarHelper.avatarResId(data.userId)),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onUserClick(data.userId) }
                    .size(WanSpacing.avatarSize),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .padding(start = WanSpacing.cardItemGap, end = WanSpacing.cardItemGap),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = data.displayName,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.niceDate,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            data.tags?.let { tags ->
                if (tags.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val uriString = "https://www.wanandroid.com${tags[0].url}"
                            val uri = uriString.toUri()
                            var cid = uri.getQueryParameter("cid")
                            if (cid.isNullOrBlank()) {
                                val paths = uri.pathSegments
                                if (paths != null && paths.size >= 3) {
                                    cid = paths[2]
                                }
                            }
                            onTagClick(cid ?: "0")
                        },
                        modifier = Modifier.height(WanSpacing.buttonSmallHeight),
                        shape = RoundedCornerShape(WanSpacing.tagCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer),
                        contentPadding = PaddingValues(3.dp, 2.dp, 3.dp, 2.dp)
                    ) {
                        Text(
                            text = tags[0].name,
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(WanSpacing.cardItemGap))
        Row(
            modifier = Modifier.padding(start = WanSpacing.cardHorizontal, end = WanSpacing.cardHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (data.desc.isNotBlank()) {
                    Text(
                        text = data.title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = data.desc,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = data.title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (data.envelopePic.isNotBlank()) {
                AsyncImage(
                    model = data.envelopePic,
                    contentDescription = null,
                    modifier = Modifier
                        .width(WanSpacing.thumbnailWidth)
                        .padding(start = WanSpacing.cardItemGap)
                        .aspectRatio(2f / 3f),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.size(WanSpacing.cardSmallGap))
        Row(
            modifier = Modifier.padding(start = WanSpacing.cardHorizontal, end = WanSpacing.cardHorizontal, bottom = WanSpacing.cardHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val footModifier = Modifier.wrapContentHeight()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = WanSpacing.cardFooterEndPadding)
            ) {
                if (data.fresh) {
                    Text(
                        text = "新  ",
                        modifier = footModifier.clickable { onChapterClick(data.chapterId) },
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (data.top) {
                    Text(
                        text = "置顶  ",
                        modifier = footModifier.clickable { onChapterClick(data.chapterId) },
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = data.chapterName,
                    modifier = footModifier.clickable { onChapterClick(data.chapterId) },
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Image(
                painter = painterResource(id = collectResId),
                contentDescription = "",
                modifier = footModifier
                    .height(WanSpacing.buttonSmallHeight)
                    .clickable {
                        scope.launch {
                            onCollectClick(data.id, !collected)
                            collected = !collected
                        }
                    })
        }
    }
}

private fun getCollectResId(collect: Boolean): Int {
    return when (collect) {
        true -> R.mipmap.ic_collect_checked
        false -> R.mipmap.ic_collect_unchecked
    }
}
