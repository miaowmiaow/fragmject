package com.example.fragmject.feature.user.ui.mycoin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.designsystem.NAV_TRANSITION_DURATION_MS
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.CollapsingHeader
import com.example.fragmject.core.ui.components.CollapsingHeaderState
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.contentPhase
import com.example.fragmject.core.ui.components.rememberCollapsingHeaderState
import com.example.fragmject.core.ui.utils.getScreenWidth
import com.example.fragmject.feature.user.impl.R

@Composable
fun MyCoinScreen(
    viewModel: MyCoinViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val userNavigator = LocalNavigators.current.user
    val onNavigateUp = LocalOnNavigateUp.current
    val density = LocalDensity.current
    val sw = context.getScreenWidth()
    val collapsingHeaderState = rememberCollapsingHeaderState()
    val coinOffsetXPx = (sw - collapsingHeaderState.titleBarSizePx) / 2
    val coin by viewModel.coin.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    Scaffold(
        modifier = Modifier.nestedScroll(collapsingHeaderState.nestedScrollConnection),
        topBar = {
            CollapsingHeader(
                state = collapsingHeaderState,
                onNavigateUp = onNavigateUp,
                actions = {
                    MyCoinRankAction(
                        state = collapsingHeaderState,
                        onClick = userNavigator::openRank,
                    )
                },
            ) {
                MyCoinCollapsingTitle(
                    coinCount = coin.coinCount,
                    coinOffsetXPx = coinOffsetXPx,
                    state = collapsingHeaderState,
                    density = density,
                )
            }
        },
    ) { innerPadding ->
        MyCoinList(pagingItems = pagingItems, innerPadding = innerPadding)
    }
}

/** 右上角排行榜入口。 */
@Composable
private fun BoxScope.MyCoinRankAction(
    state: CollapsingHeaderState,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier
            .height(state.titleBarSize)
            .align(Alignment.TopEnd),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(R.mipmap.ic_rank),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** 折叠头部里的「我的积分」与积分总数，随折叠进度收缩位移。 */
@Composable
private fun BoxScope.MyCoinCollapsingTitle(
    coinCount: String,
    coinOffsetXPx: Float,
    state: CollapsingHeaderState,
    density: Density,
) {
    Text(
        text = "我的积分",
        modifier = Modifier
            .offset {
                IntOffset(
                    x = -((coinOffsetXPx - state.titleBarSizePx - with(
                        density
                    ) { 10.dp.roundToPx() }) * (1 - state.targetPercent.value)).toInt(),
                    y = -(state.titleBarSizePx * state.targetPercent.value).toInt()
                )
            }
            .align(Alignment.Center),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Text(
        text = coinCount,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = -((coinOffsetXPx - state.titleBarSizePx - with(
                        density
                    ) { 75.dp.roundToPx() }) * (1 - state.targetPercent.value)).toInt(),
                    y = (with(density) { 10.dp.roundToPx() } * state.targetPercent.value).toInt()
                )
            }
            .align(Alignment.Center),
        fontSize = 64.sp * state.targetPercent.value.coerceAtLeast(0.25f),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

/** 积分明细列表。 */
@Composable
private fun MyCoinList(
    pagingItems: LazyPagingItems<MyCoin>,
    innerPadding: PaddingValues,
) {
    ContentStateBox(
        phase = pagingItems.contentPhase(),
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        minShowMillis = NAV_TRANSITION_DURATION_MS,
        onRetry = { pagingItems.retry() },
    ) {
        PagingSwipeRefreshBox(
            pagingItems = pagingItems,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            key = { it.id },
        ) { item ->
            MyCoinRow(item)
        }
    }
}

/** 单条积分记录：标题/时间 + 积分数。 */
@Composable
private fun MyCoinRow(item: MyCoin) {
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
                color = MaterialTheme.colorScheme.onSecondary,
            )
            Text(
                text = item.getTime(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }
        Text(
            text = item.coinCount,
            fontSize = 14.sp,
            color = AppColors.orange,
        )
    }
    HorizontalDivider()
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyCoinScreenPreview() {
    AppTheme { MyCoinScreen() }
}

/**
 * MyCoin 的 UI 展示扩展：从 desc 中解析出时间与标题。
 *
 * 只在本 Screen 内使用，故为 private（原先是独立文件里的 public 扩展，
 * 会让「只服务于一个页面」的格式化逻辑看起来像可复用工具）。
 * 保持在 feature:user 的 UI 层（而非 core:model），避免领域模型承载字符串解析展示逻辑。
 */
private fun MyCoin.getTime(): String = desc.substring(0, secondSpace())

private fun MyCoin.getTitle(): String = desc.substring(secondSpace() + 1)

private fun MyCoin.firstSpace(): Int = desc.indexOf(" ")

private fun MyCoin.secondSpace(): Int = desc.indexOf(" ", firstSpace() + 1)
