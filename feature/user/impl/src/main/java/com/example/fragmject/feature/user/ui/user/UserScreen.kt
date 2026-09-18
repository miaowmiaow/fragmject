package com.example.fragmject.feature.user.ui.user

import androidx.navigation3.runtime.NavKey
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.CollapsingHeader
import com.example.fragmject.core.ui.components.SwipeRefreshBox
import com.example.fragmject.core.ui.components.rememberCollapsingHeaderState
import com.example.fragmject.core.ui.utils.getScreenWidth
import com.example.fragmject.core.ui.components.ArticleCard
import com.example.fragmject.core.ui.components.toArticleCardUiState
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.user.UserNavKey
import com.example.fragmject.core.ui.utils.AvatarUtils

@Composable
fun UserScreen(
    userId: String,
    viewModel: UserViewModel = viewModel(key = userId),
    onNavigate: (key: NavKey) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { viewModel.init(userId) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val sw = context.getScreenWidth()
    val collapsingHeaderState = rememberCollapsingHeaderState()
    val avatarOffsetXPx = (sw - collapsingHeaderState.titleBarSizePx) / 2
    Scaffold(
        modifier = Modifier.nestedScroll(collapsingHeaderState.nestedScrollConnection),
        topBar = {
            CollapsingHeader(
                state = collapsingHeaderState,
                onNavigateUp = onNavigateUp,
            ) {
                Image(
                    painter = painterResource(id = AvatarUtils.avatarResId(uiState.coinResult.userId)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -((avatarOffsetXPx - collapsingHeaderState.titleBarSizePx) * (1 - collapsingHeaderState.targetPercent.value)).toInt(),
                                y = 0
                            )
                        }
                        .clip(CircleShape)
                        .size(collapsingHeaderState.titleBarSize * collapsingHeaderState.targetPercent.value.coerceAtLeast(0.75f))
                        .align(Alignment.Center)
                )
                Text(
                    text = uiState.coinResult.nickname,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -((avatarOffsetXPx - (collapsingHeaderState.titleBarSizePx * 2)) * (1 - collapsingHeaderState.targetPercent.value)).toInt(),
                                y = (with(density) { 35.dp.roundToPx() } * collapsingHeaderState.targetPercent.value).toInt()
                            )
                        }
                        .align(Alignment.Center),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "积分:${uiState.coinResult.coinCount}",
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (with(density) { 55.dp.roundToPx() } * collapsingHeaderState.targetPercent.value).toInt()
                            )
                        }
                        .graphicsLayer {
                            alpha = collapsingHeaderState.targetPercent.value
                        }
                        .align(Alignment.Center),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { innerPadding ->
        key(userId) {
            SwipeRefreshBox(
                items = uiState.articleResult,
                isRefreshing = uiState.isRefreshing,
                hasMore = uiState.isLoading,
                isFinishing = uiState.isFinishing,
                onRefresh = { viewModel.getHome() },
                onLoad = { viewModel.getNext() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                key = { _, item -> item.id },
            ) { _, item ->
                ArticleCard(
                    data = remember(item.id) { item.toArticleCardUiState() },
                    onArticleClick = { onNavigate(WebNavKey(it)) },
                    onUserClick = { onNavigate(UserNavKey(it)) },
                    onChapterClick = { onNavigate(SystemNavKey(it)) },
                    onTagClick = { onNavigate(SystemNavKey(it)) },
                    onCollectClick = viewModel.collectAction,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun UserScreenPreview() {
    AppTheme { UserScreen("0") }
}