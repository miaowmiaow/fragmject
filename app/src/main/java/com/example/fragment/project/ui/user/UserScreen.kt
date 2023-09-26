package com.example.fragment.project.ui.user

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
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
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.SwipeRefresh
import com.example.miaow.base.utils.getScreenWidth
import com.example.miaow.base.utils.px2dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun UserScreen(
    userId: String,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sw = context.getScreenWidth()
    val titleBarSize = 45.dp
    val titleBarSizePx = with(LocalDensity.current) { titleBarSize.roundToPx().toFloat() }
    val avatarOffsetXPx = (sw - titleBarSizePx) / 2
    val avatarOffsetX = Dp(context.px2dp(avatarOffsetXPx))
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

    val viewModel: UserViewModel = viewModel(
        factory = UserViewModel.provideFactory(userId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .background(colorResource(R.color.theme))
                .fillMaxWidth()
                .height(titleBarSize + targetHeight * targetPercent.value)
        ) {
            IconButton(
                modifier = Modifier.height(titleBarSize),
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
            Image(
                painter = painterResource(id = uiState.coinResult.getAvatarId()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(titleBarSize * targetPercent.value.coerceAtLeast(0.75f))
                    .align(Alignment.Center)
                    .offset(x = -(avatarOffsetX - titleBarSize) * (1 - targetPercent.value))
                    .clip(CircleShape),
            )
            Text(
                text = uiState.coinResult.nickname,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -(avatarOffsetX - (titleBarSize * 2)) * (1 - targetPercent.value),
                        y = 35.dp * targetPercent.value
                    ),
                fontSize = 16.sp,
                color = colorResource(R.color.text_fff),
            )
            Text(
                text = "积分:${uiState.coinResult.coinCount}",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 0.dp, y = 55.dp * targetPercent.value)
                    .graphicsLayer {
                        alpha = targetPercent.value
                    },
                fontSize = 12.sp,
                color = colorResource(R.color.text_fff),
            )
        }
        LoadingContent(uiState.refreshing && !uiState.loading) {
            SwipeRefresh(
                items = uiState.articleResult,
                refreshing = uiState.refreshing,
                loading = uiState.loading,
                finishing = uiState.finishing,
                onRefresh = { viewModel.getHome() },
                onLoad = { viewModel.getNext() },
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.white)),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                key = { _, item -> item.id },
            ) { _, item ->
                ArticleCard(
                    data = item,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToSystem = onNavigateToSystem,
                    onNavigateToUser = {},
                    onNavigateToWeb = onNavigateToWeb,
                )
            }
        }
    }

}