package com.example.fragment.module.wan.compose

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.PullRefreshLayout
import com.example.fragment.library.common.compose.ArticleCard
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.model.TabEventViewMode
import com.example.fragment.module.wan.model.QASquareModel

@Composable
fun QASquareScreen(
    eventViewModel: TabEventViewMode = viewModel(),
    viewModel: QASquareModel = viewModel(),
    onClick: (String) -> Unit,
    avatarClick: (String) -> Unit = {},
    tagClick: (String?) -> Unit = {},
    chapterNameClick: (String) -> Unit = {},
) {
    val listState = rememberLazyListState(
        eventViewModel.qaSquareFirstVisibleItemIndex(),
        eventViewModel.qaSquareFirstVisibleItemScrollOffset()
    )
    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            eventViewModel.setQASquareFirstVisibleItemIndex(listState.firstVisibleItemIndex)
            eventViewModel.setQASquareFirstVisibleItemScrollOffset(listState.firstVisibleItemScrollOffset)
        }
    }

    PullRefreshLayout(
        modifier = Modifier.fillMaxSize(),
        listState = listState,
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        refreshing = viewModel.refreshing,
        onRefresh = {
            viewModel.getUserArticleHome()
        },
        loading = viewModel.loading,
        onLoad = {
            viewModel.getUserArticleNext()
        },
        items = viewModel.userArticleResult,
    ) { index, item ->
        ArticleCard(
            index,
            item,
            onClick = { onClick(item.link) },
            avatarClick = { avatarClick(item.userId) },
            tagClick = {
                val uriString = "https://www.wanandroid.com${item.tags?.get(0)?.url}"
                val uri = Uri.parse(uriString)
                var cid = uri.getQueryParameter(Keys.CID)
                if (cid.isNullOrBlank()) {
                    val paths = uri.pathSegments
                    if (paths != null && paths.size >= 3) {
                        cid = paths[2]
                    }
                }
                tagClick(cid)
            },
            chapterNameClick = { chapterNameClick(item.chapterId) }
        )
    }
}