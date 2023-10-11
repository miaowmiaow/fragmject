package com.example.fragment.project.ui.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.Banner
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.SwipeRefresh

@Composable
fun HomeScreen(
    listState: LazyListState,
    viewModel: HomeViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadingContent(uiState.refreshing && !uiState.loading) {
        SwipeRefresh(
            items = uiState.result,
            refreshing = uiState.refreshing,
            loading = uiState.loading,
            finishing = uiState.finishing,
            onRefresh = { viewModel.getHome() },
            onLoad = { viewModel.getNext() },
            modifier = Modifier.fillMaxSize(),
            listState = listState,
            contentPadding = PaddingValues(top = 10.dp),
            key = { _, item -> item.id },
            contentType = { _, item -> item.viewType },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { _, item ->
            if (item.viewType == 0) {
                Banner(
                    data = item.banners,
                    pathMapping = { it.imagePath },
                    onClick = { _, banner -> onNavigateToWeb(banner.url) }
                )
            } else {
                ArticleCard(
                    data = item,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToUser = onNavigateToUser,
                    onNavigateToSystem = onNavigateToSystem,
                    onNavigateToWeb = onNavigateToWeb
                )
            }
        }
    }
}
