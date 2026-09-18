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
import com.example.fragmject.core.model.ArticleTag
import com.example.fragmject.core.ui.R
import com.example.fragmject.core.designsystem.AppSpacing
import kotlinx.coroutines.launch

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
            .clip(RoundedCornerShape(AppSpacing.cardCornerRadius))
            .clipToBounds()
            .clickable { onArticleClick(data.link) }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = AppSpacing.cardHorizontal, top = AppSpacing.cardHorizontal, end = AppSpacing.cardHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = data.avatarResId),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onUserClick(data.userId) }
                    .size(AppSpacing.avatarSize),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .padding(start = AppSpacing.cardItemGap, end = AppSpacing.cardItemGap),
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
                        onClick = { onTagClick(tags[0].extractCid()) },
                        modifier = Modifier.height(AppSpacing.buttonSmallHeight),
                        shape = RoundedCornerShape(AppSpacing.tagCornerRadius),
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
        Spacer(Modifier.size(AppSpacing.cardItemGap))
        Row(
            modifier = Modifier.padding(start = AppSpacing.cardHorizontal, end = AppSpacing.cardHorizontal),
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
                        .width(AppSpacing.thumbnailWidth)
                        .padding(start = AppSpacing.cardItemGap)
                        .aspectRatio(2f / 3f),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.size(AppSpacing.cardSmallGap))
        Row(
            modifier = Modifier.padding(start = AppSpacing.cardHorizontal, end = AppSpacing.cardHorizontal, bottom = AppSpacing.cardHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val footModifier = Modifier.wrapContentHeight()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = AppSpacing.cardFooterEndPadding)
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
                    .height(AppSpacing.buttonSmallHeight)
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

/**
 * 从 [ArticleTag.url] 中提取文章分类 cid。
 *
 * URL 格式通常为 `https://www.wanandroid.com/project/list/1?cid=294`，
 * 优先通过 query 参数 "cid" 获取，其次从 pathSegments 提取。
 */
private fun ArticleTag.extractCid(): String {
    val uriString = "https://www.wanandroid.com$url"
    val uri = uriString.toUri()
    var cid = uri.getQueryParameter("cid")
    if (cid.isNullOrBlank()) {
        val paths = uri.pathSegments
        if (paths != null && paths.size >= 3) {
            cid = paths[2]
        }
    }
    return cid ?: "0"
}
