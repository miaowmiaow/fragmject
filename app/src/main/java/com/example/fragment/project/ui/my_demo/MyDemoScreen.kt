package com.example.fragment.project.ui.my_demo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fragment.project.components.TabBar
import com.example.fragment.project.components.TitleBar
import com.example.miaow.base.utils.getMetaData
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyDemoScreen(
    onNavigateUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("权限", "滚轮", "相册", "全文")
    val pagerState = rememberPagerState { tabs.size }
    Column(
        modifier = Modifier.systemBarsPadding()
    ) {
        TitleBar(context.getMetaData("app_channel")) { onNavigateUp() }
        TabBar(
            data = tabs,
            textMapping = { it },
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
        )
        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> PermissionScreen()
                1 -> DatePickerScreen()
                2 -> PictureSelectorScreen()
                3 -> FullTextScreen()
            }
        }
    }
}

