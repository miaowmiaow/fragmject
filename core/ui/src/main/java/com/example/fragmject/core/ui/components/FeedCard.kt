package com.example.fragmject.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
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
import coil.compose.AsyncImage
import com.example.fragmject.core.designsystem.AppSpacing
import com.example.fragmject.core.ui.R
import kotlinx.coroutines.launch

/**
 * 通用信息流卡片组件——零业务语义、零模型依赖。
 *
 * 由 ArticleCard 泛化而来：移除文章特有的「章节 / 新 / 置顶 / 分类」等业务概念，
 * 统一抽象为 footer（底部信息 + 可点击项）与 footerBadges（角标）两个通用槽位。
 * 所有业务语义由调用方在映射层（Article → FeedCardUiState）翻译。
 *
 * @param data          卡片展示数据（[FeedCardUiState]）
 * @param onItemClick   点击卡片主体 → 参数为跳转链接 link
 * @param onUserClick   点击头像 → 参数为 userId
 * @param onFooterClick 点击底部信息/角标区域 → 参数为 footerId
 * @param onToggleClick 点击选中切换按钮 → 参数为 (id, 选中态)
 */
@Composable
fun FeedCard(
    data: FeedCardUiState,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onFooterClick: (String) -> Unit = {},
    onToggleClick: suspend (String, Boolean) -> Unit = { _, _ -> },
) {
    val scope = rememberCoroutineScope()
    var selected by remember(data.id) { mutableStateOf(data.selected) }
    val toggleResId = getToggleResId(selected)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppSpacing.cardCornerRadius))
            .clipToBounds()
            .clickable { onItemClick(data.link) }
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
                    text = data.date,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            if (data.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = data.coverUrl,
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
                data.footerBadges.forEach { badge ->
                    Text(
                        text = "${badge.text}  ",
                        modifier = footModifier.clickable { onFooterClick(data.footerId) },
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = data.footerText,
                    modifier = footModifier.clickable { onFooterClick(data.footerId) },
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Image(
                painter = painterResource(id = toggleResId),
                contentDescription = "",
                modifier = footModifier
                    .height(AppSpacing.buttonSmallHeight)
                    .clickable {
                        scope.launch {
                            onToggleClick(data.id, !selected)
                            selected = !selected
                        }
                    })
        }
    }
}

private fun getToggleResId(selected: Boolean): Int {
    return when (selected) {
        true -> R.mipmap.ic_collect_checked
        false -> R.mipmap.ic_collect_unchecked
    }
}
