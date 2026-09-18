package com.example.fragmject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 首屏加载骨架屏：以灰块模拟列表结构，替代「转圈 + 空白」。
 * 每个骨架块独立叠加 shimmer 扫光（一条亮带从左到右），
 * 网络差、加载久时也能有明确动效反馈。
 */
@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    contentPadding: PaddingValues = PaddingValues(10.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(itemCount) {
            SkeletonArticleCard()
        }
    }
}

@Composable
private fun SkeletonArticleCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
    ) {
        // 头部：头像 + 用户名单 + 日期单
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(
                modifier = Modifier.size(28.dp),
                shape = CircleShape
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkeletonBlock(Modifier.width(80.dp).height(12.dp))   // 用户名单
                SkeletonBlock(Modifier.width(56.dp).height(12.dp))   // 日期单
            }
        }
        Spacer(Modifier.height(12.dp))
        // 主体：标题单 + 描述单
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.9f).height(14.dp))  // 标题单
            SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(14.dp))  // 描述单
        }
    }
}

