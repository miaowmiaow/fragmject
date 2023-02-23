package com.example.fragment.project.ui.main.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fragment.project.R

@Composable
fun UserScreen(
    viewModel: UserViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToMyCoin: () -> Unit = {},
    onNavigateToMyCollect: () -> Unit = {},
    onNavigateToMyInfo: () -> Unit = {},
    onNavigateToMyShare: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
) {
    val userUiState by viewModel.uiState.collectAsStateWithLifecycle()
    SideEffect {
        viewModel.getUser()
    }
    Column(
        modifier = Modifier
            .background(colorResource(R.color.background))
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = userUiState.userBean.avatar.ifBlank { R.drawable.avatar_1_raster },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(top = 50.dp)
                .size(75.dp)
                .clip(RoundedCornerShape(50))
                .clickable {
                    if (userUiState.userBean.id.isNotBlank())
                        onNavigateToMyInfo()
                    else
                        onNavigateToLogin()
                }
        )
        Text(
            text = userUiState.userBean.username.ifBlank { "去登录" },
            modifier = Modifier
                .padding(top = 5.dp, bottom = 25.dp)
                .clickable {
                    if (userUiState.userBean.id.isNotBlank())
                        onNavigateToMyInfo()
                    else
                        onNavigateToLogin()
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
        Row(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .fillMaxWidth()
                .height(45.dp)
                .clickable {
                    onNavigateToMyCoin()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "我的积分",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 25.dp, end = 25.dp),
                fontSize = 13.sp,
                color = colorResource(R.color.text_333),
            )
            Image(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "",
                modifier = Modifier.padding(start = 25.dp, end = 25.dp)
            )
        }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        Row(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .fillMaxWidth()
                .height(45.dp)
                .clickable {
                    onNavigateToMyCollect()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "我的收藏",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 25.dp, end = 25.dp),
                fontSize = 13.sp,
                color = colorResource(R.color.text_333),
            )
            Image(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "",
                modifier = Modifier.padding(start = 25.dp, end = 25.dp)
            )
        }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        Row(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .fillMaxWidth()
                .height(45.dp)
                .clickable {
                    onNavigateToMyShare()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "我的分享",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 25.dp, end = 25.dp),
                fontSize = 13.sp,
                color = colorResource(R.color.text_333),
            )
            Image(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "",
                modifier = Modifier.padding(start = 25.dp, end = 25.dp)
            )
        }
        Spacer(
            Modifier
                .background(colorResource(R.color.line))
                .fillMaxWidth()
                .height(1.dp)
        )
        Row(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .fillMaxWidth()
                .height(45.dp)
                .clickable {
                    onNavigateToSetting()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "系统设置",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 25.dp, end = 25.dp),
                fontSize = 13.sp,
                color = colorResource(R.color.text_333),
            )
            Image(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "",
                modifier = Modifier.padding(start = 25.dp, end = 25.dp)
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