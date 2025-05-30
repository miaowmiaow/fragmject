package com.example.fragment.project.ui.browse_history

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.SwipeBox
import com.example.fragment.project.components.TabBar
import com.example.fragment.project.components.TitleBar
import com.example.fragment.project.components.rememberSwipeBoxControl
import kotlinx.coroutines.launch

@Composable
fun BrowseHistoryScreen(
    viewModel: BrowseHistoryViewModel = viewModel(),
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val tabs = listOf("书签", "历史")
    val pagerState = rememberPagerState { tabs.size }
    val control = rememberSwipeBoxControl()
    Scaffold(
        topBar = {
            TitleBar(
                title = "浏览历史",
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
            )
        }
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
            HorizontalPager(
                state = pagerState,
            ) { page ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    itemsIndexed(if (page == 0) uiState.bookmarkResult else uiState.historyResult) { _, item ->
                        Row(
                            modifier = Modifier
                                .clickable {
                                    if (page == 0) {
                                        viewModel.setBrowseHistory(item.value, item.url)
                                    }
                                    onNavigateToWeb(item.url)
                                }
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
                                    Box(
                                        modifier = Modifier
                                            .background(WanTheme.blue)
                                            .fillMaxSize()
                                            .clickable {
                                                control.center()
                                                // 复制链接到剪贴板
                                                val clipboard =
                                                    context.getSystemService(ClipboardManager::class.java)
                                                val clip = ClipData.newPlainText("url", item.url)
                                                clipboard?.setPrimaryClip(clip)
                                                Toast.makeText(
                                                    context,
                                                    "已复制到剪贴板",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                control.center()
                                            }
                                    ) {
                                        Text(
                                            text = "复制链接",
                                            modifier = Modifier.align(Alignment.Center),
                                            style = TextStyle.Default.copy(
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                },
                                endFillAction = {
                                    Box(
                                        modifier = Modifier
                                            .background(WanTheme.red)
                                            .fillMaxSize()
                                            .clickable {
                                                control.center()
                                                viewModel.deleteHistory(item)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(70.dp)
                                                .fillMaxHeight()
                                        ) {
                                            Text(
                                                text = "删除",
                                                modifier = Modifier.align(Alignment.Center),
                                                style = TextStyle.Default.copy(
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 15.dp)) {
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
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun BrowseHistoryScreenPreview() {
    WanTheme { BrowseHistoryScreen() }
}