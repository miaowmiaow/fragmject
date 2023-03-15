package com.example.fragment.project.ui.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.Banner
import com.example.fragment.project.components.BoxLayout
import com.example.fragment.project.components.SwipeRefresh

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    BoxLayout(uiState.refreshing && !uiState.loading) {
        SwipeRefresh(
            modifier = Modifier.fillMaxSize(),
            listState = listState,
            contentPadding = PaddingValues(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            refreshing = uiState.refreshing,
            loading = uiState.loading,
            onRefresh = { viewModel.getHome() },
            onLoad = { viewModel.getNext() },
            onRetry = { viewModel.getHome() },
            data = uiState.result,
        ) { _, item ->
            if (item.viewType == 0) {
                Banner(
                    data = item.banners,
                    pathMapping = { it.imagePath },
                    onClick = { _, banner -> onNavigateToWeb(banner.url) }
                )
            } else {
                ArticleCard(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    item = item,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToUser = onNavigateToUser,
                    onNavigateToSystem = onNavigateToSystem,
                    onNavigateToWeb = onNavigateToWeb
                )
            }
        }
    }
}
