package com.example.fragment.project.ui.demo

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.SwipeRefreshBox
import com.example.fragment.project.components.TabBar
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NestedScroll2Screen() {
    /**
     * 我更建议使用 stickyHeader 来实现同样的效果，参考 NavScreen.kt 的 NavSystemContent
     */

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val data = mutableListOf<String>().apply {
        for (i in 0 until 100) {
            add("item $i")
        }
    }
    val targetHeight = 100.dp
    val targetHeightPx = with(density) { targetHeight.roundToPx() }
    val targetPercent by remember { mutableStateOf(Animatable(1f)) }

    val tabs = listOf("tab1", "tab2")
    val pagerState = rememberPagerState { tabs.size }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {

            var dyConsumed = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                dyConsumed += delta
                dyConsumed = dyConsumed.coerceAtMost(0f)
                val percent = dyConsumed / targetHeightPx
                scope.launch {
                    targetPercent.animateTo(1 - abs(percent.coerceIn(-1f, 0f)))
                }
                if (percent > -1 && percent < 0) {
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            Column(
                modifier = Modifier
                    .background(WanTheme.theme)
                    .fillMaxWidth()
                    .requiredHeight(targetHeight * targetPercent.value)
            ) {
                Text(
                    text = "嵌套滚动2",
                    modifier = Modifier.offset {
                        IntOffset(
                            x = 0,
                            y = -(targetHeightPx  * (1 - targetPercent.value)).toInt()
                        )
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Tab + 下拉刷新",
                    modifier = Modifier.offset {
                        IntOffset(
                            x = 0,
                            y = -(targetHeightPx  * (1 - targetPercent.value)).toInt()
                        )
                    },
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabBar(
                data = tabs,
                dataMapping = { it },
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                onClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
            HorizontalPager(state = pagerState) { _ ->
                SwipeRefreshBox(
                    items = data,
                    isRefreshing = false,
                    isLoading = false,
                    isFinishing = true,
                    onRefresh = {},
                    onLoad = {},
                    modifier = Modifier
                        .fillMaxSize(),
                    key = { _, item -> item },
                ) { _, item ->
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item,
                            fontSize = 14.sp,
                            color = WanTheme.orange,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

    }
}