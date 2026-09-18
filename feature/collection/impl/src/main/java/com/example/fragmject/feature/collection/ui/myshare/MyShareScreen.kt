package com.example.fragmject.feature.collection.ui.myshare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.feature.collection.components.ArticleListPage

@Composable
fun MyShareScreen(
    viewModel: MyShareViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArticleListPage(
        title = "我的分享",
        items = uiState.result,
        isRefreshing = uiState.isRefreshing,
        hasMore = uiState.isLoading,
        isFinishing = uiState.isFinishing,
        onRefresh = { viewModel.getHome() },
        onLoad = { viewModel.getNext() },
        onNavigate = onNavigate,
        onNavigateUp = onNavigateUp,
        onCollect = viewModel.collectAction,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyShareScreenPreview() {
    AppTheme { MyShareScreen() }
}