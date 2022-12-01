package com.example.fragment.library.base.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.pullRefreshIndicatorTransform
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> PullRefreshLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    items: List<T>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    loading: Boolean,
    onLoad: () -> Unit,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {

    val state = rememberPullRefreshState(refreshing, onRefresh)
    val rotation = animateFloatAsState(state.progress * 120)

    Box(Modifier.pullRefresh(state)) {
        LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
        ) {
            itemsIndexed(items) { index, item ->
                itemContent(index, item)
                if (loading && items.size - index < 5) {
                    LaunchedEffect(items.size) {
                        onLoad()
                    }
                }
            }
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        onLoad()
                    }
                ) {
                    Text(
                        text = if (loading) "正在加载中..." else "没有更多啦！！！",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(alignment = Alignment.Center)
                    )
                }
            }
        }
//        PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
        // Custom progress indicator
        Surface(
            modifier = Modifier
                .padding(top = 40.dp)
                .size(40.dp)
                .align(Alignment.TopCenter)
                .pullRefreshIndicatorTransform(state)
                .rotate(rotation.value),
            shape = RoundedCornerShape(10.dp),
            color = if (state.progress > 0 || refreshing) Color.DarkGray else Color.Transparent,
            elevation = if (state.progress > 0 || refreshing) 20.dp else 0.dp,
        ) {
            Box {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(25.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}