package com.example.fragment.project.ui.bookmark_history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.TabBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkHistoryScreen(
    webBookmarkData: List<String>,
    webHistoryData: List<String>,
    onWebBookmark: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onWebHistory: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("书签", "历史")
    val pagerState = rememberPagerState { tabs.size }
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(colorResource(R.color.theme))
                    .fillMaxWidth()
                    .height(45.dp)
            ) {
                IconButton(
                    modifier = Modifier.height(45.dp),
                    onClick = onNavigateUp
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colorResource(R.color.white)
                    )
                }
                TabBar(
                    data = tabs,
                    dataMapping = { it },
                    pagerState = pagerState,
                    modifier = Modifier
                        .width(200.dp)
                        .height(45.dp)
                        .align(Alignment.Center),
                    backgroundColor = colorResource(R.color.theme),
                    selectedContentColor = colorResource(R.color.theme_orange),
                    unselectedContentColor = colorResource(R.color.text_fff),
                    indicatorColor = colorResource(R.color.theme),
                    dividerColor = colorResource(R.color.transparent),
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(it) } },
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
        ) { page ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                itemsIndexed(if (page == 0) webBookmarkData else webHistoryData) { _, item ->
                    Row(
                        modifier = Modifier
                            .clickable {
                                if (page == 0) {
                                    onWebHistory(true, item)
                                }
                                onNavigateToWeb(item)
                            }
                            .background(colorResource(R.color.white))
                            .height(45.dp)
                            .padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.weight(1f),
                            color = colorResource(id = R.color.text_333),
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            painter = painterResource(
                                if (page == 0) {
                                    R.mipmap.ic_collect_checked
                                } else {
                                    R.mipmap.ic_delete
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    if (page == 0) {
                                        onWebBookmark(false, item)
                                    } else {
                                        onWebHistory(false, item)
                                    }
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

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun BookmarkHistoryScreenPreview() {
    WanTheme { BookmarkHistoryScreen(listOf(), listOf()) }
}