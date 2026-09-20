package com.example.fragmject.feature.collection.ui.mycollection

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.feature.collection.components.PagingArticleListPage

@Composable
fun MyCollectScreen(
    viewModel: MyCollectViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    PagingArticleListPage(
        title = "我的收藏",
        pagingItems = pagingItems,
        onNavigate = onNavigate,
        onNavigateUp = onNavigateUp,
        onCollect = viewModel.collectAction,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyCollectScreenPreview() {
    AppTheme { MyCollectScreen() }
}