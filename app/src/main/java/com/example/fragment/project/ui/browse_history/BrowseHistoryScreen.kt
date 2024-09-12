package com.example.fragment.project.ui.browse_history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.TabBar
import com.example.fragment.project.components.TitleBar
import kotlinx.coroutines.launch

@Composable
fun BrowseHistoryScreen(
    viewModel: BrowseHistoryViewModel = viewModel(),
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val tabs = listOf("书签", "历史")
    val pagerState = rememberPagerState { tabs.size }
    Scaffold(
        topBar = {
            TitleBar(
                title = "浏览历史",
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = colorResource(R.color.white)
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
                                        viewModel.setBrowseHistory(item.value)
                                    }
                                    onNavigateToWeb(item.value)
                                }
                                .background(colorResource(R.color.white))
                                .height(45.dp)
                                .padding(horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.value,
                                modifier = Modifier.weight(1f),
                                color = colorResource(id = R.color.text_333),
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                painter = painterResource(
                                    if (page == 0) {
                                        R.mipmap.ic_bookmark
                                    } else {
                                        R.mipmap.ic_delete
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.deleteHistory(item)
                                    }
                                    .size(30.dp)
                                    .padding(10.dp, 5.dp, 0.dp, 5.dp),
                                tint = colorResource(
                                    if (page == 0) {
                                        R.color.pink
                                    } else {
                                        R.color.theme
                                    }
                                )
                            )
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