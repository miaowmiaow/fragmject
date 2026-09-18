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
import com.example.fragmject.core.designsystem.TitleBar
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.ui.components.SwipeRefreshBox
import com.example.fragmject.core.ui.components.ArticleCard
import com.example.fragmject.core.ui.components.toArticleCardUiState
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.user.UserNavKey

/**
 * 公共「文章列表页」：统一 MyCollectScreen / MyShareScreen 重复的
 * 「标题栏 + SwipeRefreshBox + ArticleCard」骨架。
 */
@Composable
fun ArticleListPage(
    title: String,
    items: List<Article>,
    isRefreshing: Boolean,
    hasMore: Boolean,
    isFinishing: Boolean,
    onRefresh: () -> Unit,
    onLoad: () -> Unit,
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
        SwipeRefreshBox(
            items = items,
            isRefreshing = isRefreshing,
            hasMore = hasMore,
            isFinishing = isFinishing,
            onRefresh = onRefresh,
            onLoad = onLoad,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            key = { _, item -> item.id },
        ) { _, item ->
            ArticleCard(
                data = remember(item.id) { item.toArticleCardUiState() },
                onArticleClick = { onNavigate(WebNavKey(it)) },
                onUserClick = { onNavigate(UserNavKey(it)) },
                onChapterClick = { onNavigate(SystemNavKey(it)) },
                onTagClick = { onNavigate(SystemNavKey(it)) },
                onCollectClick = onCollect,
            )
        }
    }
}
