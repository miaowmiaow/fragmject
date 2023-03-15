package com.example.fragment.project.ui.my_collect

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.ArticleCard
import com.example.fragment.project.components.BoxLayout
import com.example.fragment.project.components.SwipeRefresh

@Composable
fun MyCollectScreen(
    viewModel: MyCollectViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
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
                text = "我的收藏",
                fontSize = 16.sp,
                color = colorResource(R.color.text_fff),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        BoxLayout(uiState.refreshing && !uiState.loading) {
            SwipeRefresh(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                refreshing = uiState.refreshing,
                loading = uiState.loading,
                onRefresh = { viewModel.getHome() },
                onLoad = { viewModel.getNext() },
                onRetry = { viewModel.getHome() },
                data = uiState.result,
            ) { _, item ->
                ArticleCard(
                    item = item,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToUser = onNavigateToUser,
                    onNavigateToSystem = onNavigateToSystem,
                    onNavigateToWeb = onNavigateToWeb
                )
            }
        }
    }

}