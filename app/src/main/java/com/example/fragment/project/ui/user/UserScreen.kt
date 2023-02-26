package com.example.fragment.project.ui.user

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.Loading
import com.example.fragment.project.components.SwipeRefresh

@Composable
fun UserScreen(
    userId: String,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModel.provideFactory(userId)
    )
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
                text = uiState.coinResult.username,
                fontSize = 16.sp,
                color = colorResource(R.color.text_fff),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Column(
            modifier = Modifier
                .background(colorResource(R.color.theme))
                .fillMaxWidth()
                .height(100.dp)
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = uiState.coinResult.getAvatarId()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )
            Text(
                text = "积分:${uiState.coinResult.coinCount}",
                fontSize = 12.sp,
                color = colorResource(R.color.text_fff),
            )
        }
        Loading(uiState.refreshing && !uiState.loading) {
            SwipeRefresh(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                refreshing = uiState.refreshing,
                loading = uiState.loading,
                onRefresh = { viewModel.getShareArticlesHome() },
                onLoad = { viewModel.getShareArticlesNext() },
                onRetry = { viewModel.getShareArticlesHome() },
                data = uiState.articleResult,
            ) { _, item ->
                ArticleCard(
                    item = item,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToSystem = onNavigateToSystem,
                    onNavigateToWeb = onNavigateToWeb
                )
            }
        }
    }

}