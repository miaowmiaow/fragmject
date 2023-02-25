package com.example.fragment.project.ui.rank

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.Loading
import com.example.fragment.project.components.SwipeRefresh

@Composable
fun RankScreen(
    viewModel: RankViewModel = viewModel(),
    onNavigateToUserInfo: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.systemBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(colorResource(R.color.theme))
        ) {
            IconButton(
                modifier = Modifier.height(45.dp),
                onClick = {
                    if (context is AppCompatActivity) {
                        context.onBackPressedDispatcher.onBackPressed()
                    }
                }
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colorResource(R.color.white)
                )
            }
            Text(
                text = "积分排行榜",
                fontSize = 16.sp,
                color = colorResource(R.color.text_fff),
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                modifier = Modifier
                    .height(45.dp)
                    .padding(13.dp)
                    .align(Alignment.CenterEnd),
                onClick = {
                    onNavigateToWeb("https://www.wanandroid.com/blog/show/2653")
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rule),
                    contentDescription = null,
                    tint = colorResource(R.color.white)
                )
            }
        }
        Loading(uiState.refreshing && !uiState.loading) {
            SwipeRefresh(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                refreshing = uiState.refreshing,
                loading = uiState.loading,
                onRefresh = { viewModel.getHome() },
                onLoad = { viewModel.getNext() },
                onRetry = { viewModel.getHome() },
                data = uiState.result,
            ) { _, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = item.getAvatarId()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onNavigateToUserInfo(item.userId)
                                }
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = item.username,
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_666),
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