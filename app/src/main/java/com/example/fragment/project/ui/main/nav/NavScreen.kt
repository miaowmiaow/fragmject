package com.example.fragment.project.ui.main.nav

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.TabBar
import com.example.fragment.project.data.Tree
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun NavScreen(
    systemData: List<Tree>,
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val tabs = listOf("导航", "体系")
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { tabs.size }
    Column {
        TabBar(
            data = tabs,
            dataMapping = { it },
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            onClick = { scope.launch { pagerState.animateScrollToPage(it) } },
        )
        HorizontalPager(state = pagerState) { page ->
            if (page == 0) {
                NavLinkContent(onNavigateToWeb = onNavigateToWeb)
            } else if (page == 1) {
                NavSystemContent(
                    systemData = systemData,
                    onNavigateToSystem = onNavigateToSystem
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavLinkContent(
    viewModel: NavViewModel = viewModel(),
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState { uiState.navigationResult.size }
    val scrollState = rememberScrollState()
    var velocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            val halfVisibleItemSize = listState.layoutInfo.visibleItemsInfo.size / 2 - 1
            val index = (pagerState.currentPage - halfVisibleItemSize).coerceAtLeast(0)
            listState.animateScrollToItem(index)
            scrollState.scrollTo(0)
        }
    }
    LaunchedEffect(scrollState) {
        var scrollValue = 0
        snapshotFlow { scrollState.isScrollInProgress }.collectLatest {
            if (scrollState.isScrollInProgress) {
                scrollValue = scrollState.value
            } else {
                if (velocity < -3000 && scrollState.value == 0) { //顶部上拉切换上一项
                    pagerState.scrollToPage((pagerState.currentPage - 1))
                } else if (velocity > 3000 && scrollState.value == scrollValue) { //底部上拉切换下一项
                    pagerState.scrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }
    LoadingContent(uiState.isLoading) {
        Row {
            LazyColumn(
                modifier = Modifier.width(100.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                itemsIndexed(uiState.navigationResult) { index, item ->
                    Box(
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    pagerState.scrollToPage(index)
                                }
                            }
                            .background(colorResource(if (index == pagerState.currentPage) R.color.background else R.color.white))
                            .fillMaxWidth()
                            .height(45.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.name,
                            color = colorResource(id = R.color.text_333),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            VerticalPager(state = pagerState, userScrollEnabled = false) { page ->
                FlowRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(state = scrollState, flingBehavior = object :
                            FlingBehavior {
                            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                                velocity = initialVelocity
                                return initialVelocity
                            }
                        })
                ) {
                    uiState.navigationResult.getOrNull(page)?.articles?.forEach {
                        Box(modifier = Modifier.padding(5.dp, 0.dp, 5.dp, 0.dp)) {
                            Button(
                                onClick = { onNavigateToWeb(it.link) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(R.color.gray_e5),
                                    contentColor = colorResource(R.color.text_666)
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp)
                            ) {
                                Text(
                                    text = it.title,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NavSystemContent(
    systemData: List<Tree>,
    onNavigateToSystem: (cid: String) -> Unit = {},
) {
    val listState = rememberLazyListState()
    LoadingContent(
        isLoading = systemData.isEmpty(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            systemData.forEach {
                stickyHeader {
                    Text(
                        text = it.name,
                        modifier = Modifier
                            .background(colorResource(R.color.background))
                            .fillMaxWidth()
                            .padding(15.dp, 5.dp, 15.dp, 5.dp),
                        color = colorResource(R.color.text_666),
                        fontSize = 13.sp
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier
                            .background(colorResource(R.color.background))
                            .fillMaxWidth()
                    ) {
                        it.children?.forEach { children ->
                            Box(modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)) {
                                Button(
                                    onClick = { onNavigateToSystem(children.id) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(R.color.gray_e5),
                                        contentColor = colorResource(R.color.text_666)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
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

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun NavScreenPreview() {
    WanTheme { NavScreen(systemData = listOf()) }
}