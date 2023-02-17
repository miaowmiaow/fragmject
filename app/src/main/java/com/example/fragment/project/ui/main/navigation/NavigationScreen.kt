package com.example.fragment.project.ui.main.navigation

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.FullScreenLoading
import com.example.fragment.project.R
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun NavigationScreen(
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    viewModel: NavigationViewModel = viewModel()
) {
    val tabs = arrayOf("导航", "体系")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(viewModel.getTabIndex())
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateTabIndex(pagerState.currentPage)
        }
    }
    Column {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            backgroundColor = colorResource(R.color.white),
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                    color = colorResource(R.color.theme)
                )
            },
            divider = {
                TabRowDefaults.Divider(color = colorResource(R.color.transparent))
            }
        ) {
            tabs.forEachIndexed { index, text ->
                Tab(
                    text = { Text(text) },
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    selected = pagerState.currentPage == index,
                    selectedContentColor = colorResource(R.color.theme),
                    unselectedContentColor = colorResource(R.color.text_999)
                )
            }
        }
        TabRowDefaults.Divider(color = colorResource(R.color.line))
        HorizontalPager(
            count = tabs.size,
            state = pagerState,
        ) { page ->
            if (uiState.isLoading) {
                FullScreenLoading()
            } else {
                if (tabs[page] == "导航") {
                    NavigationLinkContent(onNavigateToWeb)
                } else if (tabs[page] == "体系") {
                    NavigationSystemContent(onNavigateToSystem)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavigationLinkContent(
    onNavigateToWeb: (url: String) -> Unit = {},
    viewModel: NavigationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState(viewModel.getListScrollOffset("导航"))
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateListScrollOffset(scrollState.value, "导航")
        }
    }
    Row {
        LazyColumn(
            modifier = Modifier.width(150.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            itemsIndexed(uiState.navigationResult.toList()) { index, item ->
                Box(
                    modifier = Modifier
                        .background(
                            colorResource(if (item.isSelected) R.color.white else R.color.gray)
                        )
                        .fillMaxWidth()
                        .height(45.dp)
                        .clickable {
                            viewModel.updateSelectNavigation(index)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.name,
                        color = colorResource(id = R.color.text_333),
                        fontSize = 16.sp,
                    )
                }
            }
        }
        FlowRow(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            uiState.articlesResult.forEach {
                Box(modifier = Modifier.padding(5.dp, 0.dp, 5.dp, 0.dp)) {
                    Button(
                        onClick = {
                            onNavigateToWeb(Uri.encode(it.link))
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.gray_e5),
                            contentColor = colorResource(R.color.text_666)
                        ),
                        contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp)
                    ) {
                        Text(
                            text = it.title,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NavigationSystemContent(
    onNavigateToSystem: (cid: String) -> Unit = {},
    viewModel: NavigationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(
        viewModel.getListIndex(),
        viewModel.getListScrollOffset()
    )
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateListIndex(listState.firstVisibleItemIndex)
            viewModel.updateListScrollOffset(listState.firstVisibleItemScrollOffset)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        uiState.systemTreeResult.forEach {
            stickyHeader {
                Text(
                    text = it.name,
                    modifier = Modifier
                        .background(colorResource(R.color.gray))
                        .fillMaxWidth()
                        .padding(15.dp, 5.dp, 15.dp, 5.dp),
                    color = colorResource(R.color.text_666),
                    fontSize = 13.sp
                )
            }
            it.children?.let { children ->
                item {
                    FlowRow(
                        modifier = Modifier
                            .background(colorResource(R.color.gray))
                            .fillMaxWidth()
                    ) {
                        children.forEach { children ->
                            Box(modifier = Modifier.padding(5.dp, 0.dp, 5.dp, 0.dp)) {
                                Button(
                                    onClick = {
                                        onNavigateToSystem(children.id)
                                    },
                                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = colorResource(R.color.gray_e5),
                                        contentColor = colorResource(R.color.text_666)
                                    ),
                                    contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp)
                                ) {
                                    Text(
                                        text = children.name,
                                        fontSize = 13.sp
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