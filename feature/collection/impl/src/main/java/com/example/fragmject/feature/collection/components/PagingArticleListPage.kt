package com.example.fragmject.feature.collection.components

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.designsystem.TitleBar
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.navigation.contract.LocalArticleNavigator
import com.example.fragmject.core.navigation.contract.LocalHomeNavigator
import com.example.fragmject.core.navigation.contract.LocalUserNavigator
import com.example.fragmject.feature.collection.mapper.toFeedCardUIState

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
    val articleNavigator = LocalArticleNavigator.current
    val userNavigator = LocalUserNavigator.current
    val homeNavigator = LocalHomeNavigator.current
    val onNavigateUp = LocalOnNavigateUp.current
    val overrides by collectState.overrides.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        collectState.collectFailed.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
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
                data = remember(item.id, overrides[item.id]) { item.toFeedCardUIState(overrides[item.id]) },
                onItemClick = { articleNavigator.openArticle(it) },
                onUserClick = { userNavigator.openUserProfile(it) },
                onFooterClick = { homeNavigator.openSystemTree(it) },
                onToggleClick = collectState::toggle,
            )
        }
    }
}
