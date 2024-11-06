package com.example.fragment.project.ui.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.WanViewModel
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.SwipeRefreshBox
import com.example.fragment.project.components.TabBar
import com.example.fragment.project.components.TitleBar
import com.example.fragment.project.data.Tree
import kotlinx.coroutines.launch

@Composable
fun SystemScreen(
    cid: String,
    wanViewModel: WanViewModel = viewModel(),
    systemViewModel: SystemViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val wanUiState by wanViewModel.uiState.collectAsStateWithLifecycle()
    val treeData = wanUiState.getTree(cid)
    val pagerState = rememberPagerState(treeData.first) { treeData.third.size }
    Scaffold(
        topBar = {
            TitleBar(
                title = treeData.second,
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
                data = treeData.third,
                dataMapping = { it.name },
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
            LoadingContent(isLoading = wanUiState.isLoading) {
                HorizontalPager(state = pagerState) { page ->
                    val pageCid = treeData.third[page].id
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_START) {
                                systemViewModel.init(pageCid)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                    val systemUiState by systemViewModel.uiState.collectAsStateWithLifecycle()
                    val listState = rememberLazyListState()
                    SwipeRefreshBox(
                        items = systemUiState.getResult(pageCid),
                        isRefreshing = systemUiState.getRefreshing(pageCid),
                        isLoading = systemUiState.getLoading(pageCid),
                        isFinishing = systemUiState.getFinishing(pageCid),
                        onRefresh = { systemViewModel.getHome(pageCid) },
                        onLoad = { systemViewModel.getNext(pageCid) },
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        key = { _, item -> item.id },
                    ) { _, item ->
                        ArticleCard(
                            data = item,
                            onNavigateToLogin = onNavigateToLogin,
                            onNavigateToUser = onNavigateToUser,
                            onNavigateToSystem = onNavigateToSystem,
                            onNavigateToWeb = onNavigateToWeb
                        )
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SystemScreenPreview() {
    WanTheme { SystemScreen("") }
}