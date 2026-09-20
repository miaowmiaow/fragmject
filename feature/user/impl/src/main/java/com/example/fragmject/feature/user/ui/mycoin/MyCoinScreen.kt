package com.example.fragmject.feature.user.ui.mycoin

import androidx.navigation3.runtime.NavKey
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragmject.core.ui.R
import com.example.fragmject.feature.user.RankNavKey
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.ui.components.CollapsingHeader
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.rememberCollapsingHeaderState
import com.example.fragmject.core.ui.utils.getScreenWidth

@Composable
fun MyCoinScreen(
    viewModel: MyCoinViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
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
                    IconButton(
                        modifier = Modifier
                            .height(collapsingHeaderState.titleBarSize)
                            .align(Alignment.TopEnd),
                        onClick = { onNavigate(RankNavKey) }
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_rank),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
            ) {
                Text(
                    text = "我的积分",
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -((coinOffsetXPx - collapsingHeaderState.titleBarSizePx - with(
                                    density
                                ) { 10.dp.roundToPx() }) * (1 - collapsingHeaderState.targetPercent.value)).toInt(),
                                y = -(collapsingHeaderState.titleBarSizePx * collapsingHeaderState.targetPercent.value).toInt()
                            )
                        }
                        .align(Alignment.Center),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = coin.coinCount,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -((coinOffsetXPx - collapsingHeaderState.titleBarSizePx - with(
                                    density
                                ) { 75.dp.roundToPx() }) * (1 - collapsingHeaderState.targetPercent.value)).toInt(),
                                y = (with(density) { 10.dp.roundToPx() } * collapsingHeaderState.targetPercent.value).toInt()
                            )
                        }
                        .align(Alignment.Center),
                    fontSize = 64.sp * collapsingHeaderState.targetPercent.value.coerceAtLeast(0.25f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { innerPadding ->
        PagingSwipeRefreshBox(
            pagingItems = pagingItems,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            key = { it.id },
        ) { item ->
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
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyCoinScreenPreview() {
    AppTheme { MyCoinScreen() }
}