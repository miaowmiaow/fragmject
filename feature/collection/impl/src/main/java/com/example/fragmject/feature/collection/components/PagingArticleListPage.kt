package com.example.fragmject.feature.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.LazyPagingItems
import com.example.fragmject.core.designsystem.TitleBar
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.collection.mapper.toFeedCardUiState
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.user.UserNavKey

/**
 * 公共「文章列表页」Paging 版：统一 MyShareScreen 等页面重复的
 * 「标题栏 + PagingSwipeRefreshBox + FeedCard」骨架。
 */
@Composable
fun PagingArticleListPage(
    title: String,
    pagingItems: LazyPagingItems<Article>,
    onNavigate: (NavKey) -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onCollect: suspend (String, Boolean) -> Unit = { _, _ -> },
) {
    Scaffold(
        topBar = {
            TitleBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PagingSwipeRefreshBox(
            pagingItems = pagingItems,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            key = { it.id },
        ) { item ->
            FeedCard(
                data = remember(item.id) { item.toFeedCardUiState() },
                onItemClick = { onNavigate(WebNavKey(it)) },
                onUserClick = { onNavigate(UserNavKey(it)) },
                onFooterClick = { onNavigate(SystemNavKey(it)) },
                onToggleClick = onCollect,
            )
        }
    }
}
