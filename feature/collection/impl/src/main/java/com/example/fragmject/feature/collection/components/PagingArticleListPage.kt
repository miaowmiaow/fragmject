package com.example.fragmject.feature.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.designsystem.TitleBar
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData

/**
 * 公共「文章列表页」Paging 版：统一 MyShareScreen 等页面重复的
 * 「标题栏 + PagingSwipeRefreshBox + FeedCard」骨架。
 */
@Composable
fun PagingArticleListPage(
    title: String,
    pagingItems: LazyPagingItems<Article>,
    collectState: CollectState,
) {
    val articleNavigator = LocalNavigators.current.article
    val userNavigator = LocalNavigators.current.user
    val homeNavigator = LocalNavigators.current.home
    val onNavigateUp = LocalOnNavigateUp.current
    val collectInteraction = rememberCollectInteraction(collectState)
    Scaffold(
        topBar = {
            TitleBar(
                title = title,
                onNavigateUp = onNavigateUp,
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
                data = rememberArticleFeedData(item, collectInteraction.overrides),
                onItemClick = { articleNavigator.openArticle(it) },
                onUserClick = { userNavigator.openUserProfile(it) },
                onFooterClick = { homeNavigator.openSystemTree(it) },
                onToggleClick = collectInteraction.onToggleClick,
            )
        }
    }
}
