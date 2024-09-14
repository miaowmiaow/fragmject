package com.example.fragment.project.ui.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.BannerPager
import com.example.fragment.project.components.SwipeRefreshBox

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
    SwipeRefreshBox(
        items = uiState.result,
        isRefreshing = uiState.isRefreshing,
        isLoading = uiState.isLoading,
        isFinishing = uiState.isFinishing,
        onRefresh = { viewModel.getHome() },
        onLoad = { viewModel.getNext() },
        modifier = Modifier.fillMaxSize(),
        listState = listState,
        contentPadding = PaddingValues(top = 10.dp),
        contentType = { _, item -> item.viewType },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { _, item ->
        if (item.viewType == 0) {
            BannerPager(
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

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun HomeScreenPreview() {
    WanTheme { HomeScreen(rememberLazyListState()) }
}