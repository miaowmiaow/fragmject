package com.example.fragmject.feature.user.ui.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.designsystem.NAV_TRANSITION_DURATION_MS
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.CollapsingHeader
import com.example.fragmject.core.ui.components.CollapsingHeaderState
import com.example.fragmject.core.ui.components.CollectInteraction
import com.example.fragmject.core.ui.components.ContentStateBox
import com.example.fragmject.core.ui.components.FeedCard
import com.example.fragmject.core.ui.components.PagingSwipeRefreshBox
import com.example.fragmject.core.ui.components.contentPhase
import com.example.fragmject.core.ui.components.rememberCollapsingHeaderState
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import com.example.fragmject.core.ui.mapper.rememberArticleFeedData
import com.example.fragmject.core.ui.utils.AvatarUtils
import com.example.fragmject.core.ui.utils.getScreenWidth

@Composable
fun UserScreen(
    userId: String,
    viewModel: UserViewModel = hiltViewModel(key = userId),
) {
    val coin by viewModel.coin.collectAsStateWithLifecycle()
    val onNavigateUp = LocalOnNavigateUp.current
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val collectInteraction = rememberCollectInteraction(viewModel.collectState)
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
                UserHeaderContent(
                    coin = coin,
                    avatarOffsetXPx = avatarOffsetXPx,
                    state = collapsingHeaderState,
                    density = density,
                )
            }
        },
    ) { innerPadding ->
        key(userId) {
            UserArticleList(
                pagingItems = pagingItems,
                collectInteraction = collectInteraction,
                innerPadding = innerPadding,
            )
        }
    }
}

/** 折叠头部：头像 + 昵称 + 积分，随折叠进度收缩位移。 */
@Composable
private fun BoxScope.UserHeaderContent(
    coin: Coin,
    avatarOffsetXPx: Float,
    state: CollapsingHeaderState,
    density: Density,
) {
    Image(
        painter = painterResource(id = AvatarUtils.avatarResId(coin.userId)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .offset {
                val percent = state.targetPercent.value
                IntOffset(
                    x = -((avatarOffsetXPx - state.titleBarSizePx) * (1 - percent)).toInt(),
                    y = 0,
                )
            }
            .clip(CircleShape)
            .size(state.titleBarSize * state.targetPercent.value.coerceAtLeast(0.75f))
            .align(Alignment.Center)
    )
    Text(
        text = coin.nickname,
        modifier = Modifier
            .offset {
                val percent = state.targetPercent.value
                val titleBarDouble = state.titleBarSizePx * 2
                IntOffset(
                    x = -((avatarOffsetXPx - titleBarDouble) * (1 - percent)).toInt(),
                    y = (with(density) { 35.dp.roundToPx() } * percent).toInt(),
                )
            }
            .align(Alignment.Center),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Text(
        text = "积分:${coin.coinCount}",
        modifier = Modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = (with(density) { 55.dp.roundToPx() } * state.targetPercent.value).toInt()
                )
            }
            .graphicsLayer {
                alpha = state.targetPercent.value
            }
            .align(Alignment.Center),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

/** 用户分享的文章列表。 */
@Composable
private fun UserArticleList(
    pagingItems: LazyPagingItems<Article>,
    collectInteraction: CollectInteraction,
    innerPadding: PaddingValues,
) {
    val articleNavigator = LocalNavigators.current.article
    val homeNavigator = LocalNavigators.current.home
    val userNavigator = LocalNavigators.current.user
    val pictureNavigator = LocalNavigators.current.picture
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
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            key = { it.id },
        ) { item ->
            FeedCard(
                data = rememberArticleFeedData(item, collectInteraction.overrides),
                onItemClick = { articleNavigator.openArticle(it) },
                onUserClick = { userNavigator.openUserProfile(it) },
                onFooterClick = { homeNavigator.openSystemTree(it) },
                onToggleClick = collectInteraction.onToggleClick,
                onCoverClick = { url -> pictureNavigator.openPicturePreview(listOf(url)) },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun UserScreenPreview() {
    AppTheme { UserScreen("0") }
}
