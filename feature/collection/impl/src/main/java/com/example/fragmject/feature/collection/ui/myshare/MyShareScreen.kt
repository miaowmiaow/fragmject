package com.example.fragmject.feature.collection.ui.myshare

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.feature.collection.components.PagingArticleListPage

@Composable
fun MyShareScreen(
    viewModel: MyShareViewModel = viewModel(),
) {
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    PagingArticleListPage(
        title = "我的分享",
        pagingItems = pagingItems,
        onCollect = viewModel::collect,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyShareScreenPreview() {
    AppTheme { MyShareScreen() }
}