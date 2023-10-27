package com.example.fragment.project.ui.bookmark_history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
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
    val pagerState = rememberPagerState { 2 }
    Column {
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
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colorResource(R.color.white)
                )
            }
            TabBar(
                data = listOf("书签", "历史"),
                textMapping = { it },
                pagerState = pagerState,
                modifier = Modifier
                    .width(200.dp)
                    .height(45.dp)
                    .align(Alignment.Center),
                backgroundColor = colorResource(R.color.theme),
                selectedContentColor = colorResource(R.color.text_fff),
                unselectedContentColor = colorResource(R.color.text_ccc),
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(it) } },
            )
        }
        TabRowDefaults.Divider(color = colorResource(R.color.line))
        HorizontalPager(state = pagerState) { page ->
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