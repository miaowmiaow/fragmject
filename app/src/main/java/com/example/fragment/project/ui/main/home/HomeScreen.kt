package com.example.fragment.project.ui.main.home

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.Banner
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.library.base.compose.SwipeRefresh
import com.example.fragment.project.components.ArticleCard

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUserInfo: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(
        viewModel.getListIndex(),
        viewModel.getListScrollOffset()
    )
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateListIndex(listState.firstVisibleItemIndex)
            viewModel.updateListScrollOffset(listState.firstVisibleItemScrollOffset)
        }
    }
    if (uiState.refreshing && !uiState.loading) {
        FullScreenLoading()
    } else {
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
                    onClick = { _, banner ->
                        onNavigateToWeb(Uri.encode(banner.url))
                    }
                )
            } else {
                ArticleCard(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    item = item,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToUserInfo = onNavigateToUserInfo,
                    onNavigateToSystem = onNavigateToSystem,
                    onNavigateToWeb = onNavigateToWeb
                )
            }
        }
    }
}
