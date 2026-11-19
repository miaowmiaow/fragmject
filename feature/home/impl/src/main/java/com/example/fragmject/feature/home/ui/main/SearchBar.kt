package com.example.fragmject.feature.home.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.ui.components.LoopVerticalPager

// =====================================================================
// SearchBar
// =====================================================================

/**
 * 顶部搜索栏：左侧热词轮播（点击进搜索），右侧「投稿/分享」入口。
 *
 * 从 [MainScreen] 拆出：该文件已触达规则 28 的 500 行上限，而搜索栏与主屏的
 * 窗口尺寸分发逻辑无关，仅被 `ContentPane` 与 Compact 布局复用。
 */
@Composable
fun SearchBar(
    data: List<HotKey>,
    autoScrollEnabled: Boolean = true,
) {
    val searchNavigator = LocalNavigators.current.search
    val collectionNavigator = LocalNavigators.current.collection
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .fillMaxWidth()
            .height(45.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(15.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clipToBounds()
                .background(AppColors.alphaGray)
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(10.dp, 5.dp, 0.dp, 5.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LoopVerticalPager(
                data = data,
                userScrollEnabled = false,
                autoScrollEnabled = autoScrollEnabled,
            ) { _, _, item ->
                Box(
                    modifier = Modifier
                        .clickable { searchNavigator.openSearch(item.name) }
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = item.name,
                        modifier = Modifier.padding(start = 40.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        IconButton(
            modifier = Modifier.height(45.dp),
            onClick = { collectionNavigator.openShareArticle() },
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SearchBarPreview() {
    AppTheme { SearchBar(data = listOf(HotKey(name = "问答"))) }
}
