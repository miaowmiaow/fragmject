package com.example.fragment.project.ui.main.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fragment.project.R
import com.example.fragment.project.components.ArrowRightItem

@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToMyCoin: () -> Unit = {},
    onNavigateToMyCollect: () -> Unit = {},
    onNavigateToMyDemo: () -> Unit = {},
    onNavigateToMyShare: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        viewModel.getUser()
        onDispose {}
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = uiState.userBean.avatar.ifBlank { R.drawable.avatar_1_raster },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(top = 50.dp)
                .size(75.dp)
                .clip(RoundedCornerShape(50))
                .clickable {
                    if (uiState.isLogin()) {
                        onNavigateToUser(uiState.userBean.id)
                    } else {
                        onNavigateToLogin()
                    }
                }
        )
        Text(
            text = uiState.userBean.username.ifBlank { "去登录" },
            modifier = Modifier
                .padding(top = 5.dp, bottom = 25.dp)
                .clickable {
                    if (uiState.isLogin()) {
                        onNavigateToUser(uiState.userBean.id)
                    } else {
                        onNavigateToLogin()
                    }
                },
            fontSize = 16.sp,
            color = colorResource(R.color.text_333),
        )
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        ArrowRightItem("我的Demo") { onNavigateToMyDemo() }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        ArrowRightItem("我的积分") { onNavigateToMyCoin() }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        ArrowRightItem("我的收藏") { onNavigateToMyCollect() }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        ArrowRightItem("我的分享") { onNavigateToMyShare() }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        ArrowRightItem("系统设置") { onNavigateToSetting() }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
    }
}