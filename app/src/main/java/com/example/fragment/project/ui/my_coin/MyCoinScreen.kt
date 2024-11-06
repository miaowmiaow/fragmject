package com.example.fragment.project.ui.my_coin

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.SwipeRefreshBox
import com.example.miaow.base.utils.getScreenWidth
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MyCoinScreen(
    viewModel: MyCoinViewModel = viewModel(),
    onNavigateToRank: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val sw = context.getScreenWidth()
    val titleBarSize = 45.dp
    val titleBarSizePx = with(density) { titleBarSize.roundToPx() }
    val coinOffsetXPx = (sw - titleBarSizePx) / 2
    val targetHeight = 100.dp
    val targetHeightPx = with(density) { targetHeight.roundToPx() }
    val targetPercent by remember { mutableStateOf(Animatable(1f)) }

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            Box(
                modifier = Modifier
                    .background(WanTheme.theme)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(titleBarSize + targetHeight * targetPercent.value)
            ) {
                IconButton(
                    modifier = Modifier.height(45.dp),
                    onClick = onNavigateUp
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    modifier = Modifier
                        .height(45.dp)
                        .align(Alignment.TopEnd),
                    onClick = onNavigateToRank
                ) {
                    Icon(
                        painter = painterResource(R.mipmap.ic_rank),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = "我的积分",
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -((coinOffsetXPx - titleBarSizePx - with(density) { 10.dp.roundToPx() }) * (1 - targetPercent.value)).toInt(),
                                y = -(titleBarSizePx * targetPercent.value).toInt()
                            )
                        }
                        .align(Alignment.Center),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = uiState.userCoinResult.coinCount,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -((coinOffsetXPx - titleBarSizePx - with(density) { 75.dp.roundToPx() }) * (1 - targetPercent.value)).toInt(),
                                y = (with(density) { 10.dp.roundToPx() } * targetPercent.value).toInt()
                            )
                        }
                        .align(Alignment.Center),
                    fontSize = 64.sp * targetPercent.value.coerceAtLeast(0.25f),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    ) { innerPadding ->
        SwipeRefreshBox(
            items = uiState.myCoinResult,
            isRefreshing = uiState.isRefreshing,
            isLoading = uiState.isLoading,
            isFinishing = uiState.isFinishing,
            onRefresh = { viewModel.getHome() },
            onLoad = { viewModel.getNext() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            key = { _, item -> item.id },
        ) { _, item ->
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = item.getTitle(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = item.getTime(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Text(
                    text = item.coinCount,
                    fontSize = 14.sp,
                    color = WanTheme.orange,
                )
            }
            HorizontalDivider()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyCoinScreenPreview() {
    WanTheme { MyCoinScreen() }
}