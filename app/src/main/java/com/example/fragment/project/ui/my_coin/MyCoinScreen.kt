package com.example.fragment.project.ui.my_coin

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.SwipeRefresh
import com.example.miaow.base.utils.getScreenWidth
import com.example.miaow.base.utils.px2dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MyCoinScreen(
    viewModel: MyCoinViewModel = viewModel(),
    onNavigateToCoinRank: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sw = context.getScreenWidth()
    val titleBarSize = 45.dp
    val titleBarSizePx = with(LocalDensity.current) { titleBarSize.roundToPx().toFloat() }
    val coinOffsetXPx = (sw - titleBarSizePx) / 2
    val coinOffsetX = Dp(context.px2dp(coinOffsetXPx))
    val targetHeight = 100.dp
    val targetHeightPx = with(LocalDensity.current) { targetHeight.roundToPx().toFloat() }
    val targetPercent by remember { mutableStateOf(Animatable(1f)) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {

            var dyConsumed = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                dyConsumed += delta
                dyConsumed = dyConsumed.coerceAtMost(0f)
                val percent = dyConsumed / targetHeightPx
                coroutineScope.launch {
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
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(titleBarSize + targetHeight * targetPercent.value)
                .background(colorResource(R.color.theme))
        ) {
            IconButton(
                modifier = Modifier.height(45.dp),
                onClick = {
                    onNavigateUp()
                }
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colorResource(R.color.white)
                )
            }
            IconButton(
                modifier = Modifier
                    .height(45.dp)
                    .padding(13.dp)
                    .align(Alignment.TopEnd),
                onClick = { onNavigateToCoinRank() }
            ) {
                Icon(
                    painter = painterResource(R.mipmap.ic_rank),
                    contentDescription = null,
                    tint = colorResource(R.color.white)
                )
            }
            Text(
                text = "我的积分",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -(coinOffsetX - titleBarSize - 10.dp) * (1 - targetPercent.value),
                        y = -titleBarSize * targetPercent.value
                    ),
                fontSize = 16.sp,
                color = colorResource(R.color.text_fff),
            )
            Text(
                text = uiState.userCoinResult.coinCount,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -(coinOffsetX - titleBarSize - 75.dp) * (1 - targetPercent.value),
                        y = 10.dp * targetPercent.value
                    ),
                fontSize = 64.sp * targetPercent.value.coerceAtLeast(0.25f),
                color = colorResource(R.color.text_fff),
            )
        }
        LoadingContent(uiState.refreshing && !uiState.loading) {
            SwipeRefresh(
                items = uiState.myCoinResult,
                refreshing = uiState.refreshing,
                loading = uiState.loading,
                finishing = uiState.finishing,
                onRefresh = { viewModel.getHome() },
                onLoad = { viewModel.getNext() },
                modifier = Modifier.fillMaxSize(),
                key = { _, item -> item.id },
            ) { _, item ->
                Row(
                    modifier = Modifier
                        .background(colorResource(R.color.white))
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = item.getTitle(),
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_666),
                        )
                        Text(
                            text = item.getTime(),
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_999),
                        )
                    }
                    Text(
                        text = item.coinCount,
                        fontSize = 14.sp,
                        color = colorResource(R.color.orange),
                    )
                }
                Spacer(
                    Modifier
                        .background(colorResource(R.color.line))
                        .fillMaxWidth()
                        .height(1.dp)
                )
            }
        }
    }

}