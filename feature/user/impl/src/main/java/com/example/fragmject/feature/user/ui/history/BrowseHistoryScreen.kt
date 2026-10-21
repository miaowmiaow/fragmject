package com.example.fragmject.feature.user.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.model.History
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.SwipeBox
import com.example.fragmject.core.ui.components.TabBar
import com.example.fragmject.core.ui.components.TitleBar
import com.example.fragmject.core.ui.components.rememberSwipeBoxControl
import kotlinx.coroutines.launch

@Composable
fun BrowseHistoryScreen(
    viewModel: BrowseHistoryViewModel = hiltViewModel(),
) {
    val articleNavigator = LocalNavigators.current.article
    val onNavigateUp = LocalOnNavigateUp.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val tabs = listOf("历史", "书签")
    val pagerState = rememberPagerState { tabs.size }
    Scaffold(
        topBar = {
            TitleBar(
                title = "浏览历史",
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabBar(
                data = tabs,
                dataMapping = { it },
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
            )
            HorizontalPager(state = pagerState) { page ->
                HistoryList(
                    items = if (page == 0) uiState.historyResult else uiState.bookmarkResult,
                    onOpen = { item ->
                        // 仅书签页（page == 1）点击时记录一次浏览：历史页条目本身已是浏览记录
                        if (page == 1) {
                            viewModel.recordBrowseVisit(item.value, item.url)
                        }
                        articleNavigator.openArticle(item.url)
                    },
                    onDelete = viewModel::deleteHistory,
                )
            }
        }
    }
}

/** 单页列表：历史 / 书签共用同一套行样式与侧滑动作。 */
@Composable
private fun HistoryList(
    items: List<History>,
    onOpen: (History) -> Unit,
    onDelete: (History) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        itemsIndexed(
            items,
            key = { _, item -> item.id }
        ) { _, item ->
            HistoryRow(item = item, onOpen = onOpen, onDelete = onDelete)
        }
    }
}

/** 单条记录：整行可点击打开，右滑出「复制链接」，继续右滑出「删除」。 */
@Composable
private fun HistoryRow(
    item: History,
    onOpen: (History) -> Unit,
    onDelete: (History) -> Unit,
) {
    val context = LocalContext.current
    val control = rememberSwipeBoxControl()
    Row(
        modifier = Modifier
            .clickable { onOpen(item) }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwipeBox(
            control = control,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            actionWidth = 70.dp,
            endAction = listOf {
                HistorySwipeAction(color = AppColors.blue, label = "复制链接") {
                    control.center()
                    copyToClipboard(context, item.url)
                }
            },
            endFillAction = {
                HistorySwipeAction(
                    color = AppColors.red,
                    label = "删除",
                    fillAlignment = Alignment.CenterStart,
                ) {
                    control.center()
                    onDelete(item)
                }
            }
        ) {
            HistoryRowContent(item)
        }
    }
}

/** 侧滑动作格子；[fillAlignment] 非空时按该位置内缩一格（填充式动作）。 */
@Composable
private fun HistorySwipeAction(
    color: Color,
    label: String,
    fillAlignment: Alignment? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(color)
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        if (fillAlignment == null) {
            HistoryActionText(label)
        } else {
            Box(
                modifier = Modifier
                    .align(fillAlignment)
                    .width(70.dp)
                    .fillMaxHeight()
            ) {
                HistoryActionText(label)
            }
        }
    }
}

/** 动作文字统一样式。 */
@Composable
private fun BoxScope.HistoryActionText(text: String) {
    Text(
        text = text,
        modifier = Modifier.align(Alignment.Center),
        style = TextStyle.Default.copy(
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 12.sp
        )
    )
}

/** 条目正文：标题 + 链接。 */
@Composable
private fun HistoryRowContent(item: History) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 15.dp)
    ) {
        Text(
            text = item.value,
            color = MaterialTheme.colorScheme.onSecondary,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = item.url,
            color = MaterialTheme.colorScheme.onTertiary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 复制链接到剪贴板并提示。 */
private fun copyToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("url", url))
    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun BrowseHistoryScreenPreview() {
    AppTheme { BrowseHistoryScreen() }
}
