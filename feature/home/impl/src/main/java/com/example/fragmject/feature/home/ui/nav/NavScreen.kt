package com.example.fragmject.feature.home.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.ContentPhase
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.SkeletonRows
import com.example.fragmject.core.ui.components.TabBar
import com.example.fragmject.core.ui.utils.RefreshableUiState
import com.example.fragmject.core.ui.utils.dataOrEmpty
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun NavScreen() {
    val viewModel: NavViewModel = hiltViewModel()
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
                NavLinkContent(viewModel)
            } else if (page == 1) {
                NavSystemContent(viewModel)
            }
        }
    }
}

@Composable
fun NavLinkContent(
    viewModel: NavViewModel,
) {
    val navigators = LocalNavigators.current
    val onArticleClick = remember(navigators) { { url: String -> navigators.article.openArticle(url) } }
    val uiState by viewModel.navState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState { uiState.dataOrEmpty().size }
    var velocity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(listState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            val halfVisibleItemSize = listState.layoutInfo.visibleItemsInfo.size / 2 - 1
            val index = (pagerState.currentPage - halfVisibleItemSize).coerceAtLeast(0)
            listState.animateScrollToItem(index)
        }
    }
    val phase = when (uiState) {
        is RefreshableUiState.Loading -> ContentPhase.Loading
        is RefreshableUiState.Error -> ContentPhase.Error
        is RefreshableUiState.Success ->
            if (uiState.dataOrEmpty().isEmpty()) ContentPhase.Empty else ContentPhase.Content
    }
    ContentStateBox(
        phase = phase,
        loadingContent = { SkeletonRows(it) },
        onRetry = viewModel::retry,
    ) {
        Row {
            LazyColumn(
                modifier = Modifier.width(100.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                itemsIndexed(
                    items = uiState.dataOrEmpty(),
                    key = { _, item -> item.cid }
                ) { index, item ->
                    Box(
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    pagerState.scrollToPage(index)
                                }
                            }
                            .background(if (index == pagerState.currentPage) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainer)
                            .fillMaxWidth()
                            .height(45.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            VerticalPager(state = pagerState, userScrollEnabled = false) { page ->
                val gridState = remember(page) { LazyGridState() }
                val articles = uiState.dataOrEmpty().getOrNull(page)?.articles.orEmpty()
                // ScrollableDefaults.flingBehavior() 本身是 @Composable（读 LocalDensity），
                // 无法放进 remember；它仅被下面 remember 缓存的 FlingBehavior 捕获一次，
                // 后续的每次重组只是多一次分配，不影响已缓存的嵌套滚动行为。
                val defaultFlingBehavior = ScrollableDefaults.flingBehavior()
                val flingBehavior = remember(pagerState, gridState) {
                    object : FlingBehavior {
                        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                            velocity = initialVelocity
                            if (initialVelocity < -3000 && !gridState.canScrollBackward && pagerState.currentPage > 0) {
                                pagerState.scrollToPage(pagerState.currentPage - 1)
                                return initialVelocity
                            }
                            if (initialVelocity > 3000 && !gridState.canScrollForward && pagerState.currentPage < pagerState.pageCount - 1) {
                                pagerState.scrollToPage(pagerState.currentPage + 1)
                                return initialVelocity
                            }
                            return with(defaultFlingBehavior) { performFling(initialVelocity) }
                        }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    flingBehavior = flingBehavior,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = articles,
                        key = { item -> item.link }
                    ) { item ->
                        Button(
                            onClick = { onArticleClick(item.link) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavSystemContent(
    viewModel: NavViewModel,
) {
    val navigators = LocalNavigators.current
    val onChapterClick = remember(navigators) { { cid: String -> navigators.home.openSystemTree(cid) } }
    val listState = rememberLazyListState()
    val treeState by viewModel.treeState.collectAsStateWithLifecycle()
    val phase = when (treeState) {
        is RefreshableUiState.Loading -> ContentPhase.Loading
        is RefreshableUiState.Error -> ContentPhase.Error
        is RefreshableUiState.Success ->
            if (treeState.dataOrEmpty().isEmpty()) ContentPhase.Empty else ContentPhase.Content
    }
    ContentStateBox(
        phase = phase,
        loadingContent = { SkeletonRows(it) },
        onRetry = viewModel::retryTree,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            treeState.dataOrEmpty().forEach { data ->
                stickyHeader {
                    Text(
                        text = data.name,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxWidth()
                            .padding(15.dp, 5.dp, 15.dp, 5.dp),
                        fontSize = 13.sp
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxWidth()
                    ) {
                        data.children?.forEach { children ->
                            Box(modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)) {
                                Button(
                                    onClick = { onChapterClick(children.id) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
    AppTheme { NavScreen() }
}
