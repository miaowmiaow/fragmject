package com.example.fragment.project.ui.system

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.bean.TreeBean
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.FullScreenLoading
import com.example.fragment.project.components.SwipeRefresh
import com.example.fragment.project.components.TabBar
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SystemScreen(
    title: String = "体系",
    index: Int = 0,
    tree: List<TreeBean>,
    systemViewModel: SystemViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUserInfo: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(index)
    Column(
        modifier = Modifier
            .background(colorResource(R.color.background))
            .systemBarsPadding()
    ) {
        TitleBar(title)
        TabBar(
            pagerState = pagerState,
            data = tree,
            textMapping = { it.name },
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
        )
        HorizontalPager(
            count = tree.size,
            state = pagerState,
        ) { page ->
            val pageCid = tree[page].id
            DisposableEffect(Unit) {
                systemViewModel.init(pageCid)
                onDispose {}
            }
            val systemUiState by systemViewModel.uiState.collectAsStateWithLifecycle()
            val listState = rememberLazyListState()
            if (systemUiState.getRefreshing(pageCid) && !systemUiState.getLoading(pageCid)) {
                FullScreenLoading()
            } else {
                SwipeRefresh(
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    refreshing = systemUiState.getRefreshing(pageCid),
                    loading = systemUiState.getLoading(pageCid),
                    onRefresh = { systemViewModel.getHome(pageCid) },
                    onLoad = { systemViewModel.getNext(pageCid) },
                    onRetry = { systemViewModel.getHome(pageCid) },
                    data = systemUiState.getResult(pageCid),
                ) { _, item ->
                    ArticleCard(
                        item = item,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToUserInfo = onNavigateToUserInfo,
                        onNavigateToSystem = onNavigateToSystem,
                        onNavigateToWeb = onNavigateToWeb
                    )
                }
            }
        }
    }
}

@Composable
fun TitleBar(title: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(colorResource(R.color.theme))
    ) {
        IconButton(
            modifier = Modifier.height(45.dp),
            onClick = {
                if (context is AppCompatActivity) {
                    context.onBackPressedDispatcher.onBackPressed()
                }
            }
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = colorResource(R.color.white)
            )
        }
        Text(
            text = title,
            fontSize = 16.sp,
            color = colorResource(R.color.text_fff),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}