package com.example.fragment.project.ui.rank

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.fragment.project.R
import com.example.fragment.project.components.SwipeRefresh

@Composable
fun RankScreen(
    viewModel: RankViewModel = viewModel(),
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(colorResource(R.color.theme))
                    .fillMaxWidth()
                    .height(45.dp)
            ) {
                IconButton(
                    modifier = Modifier.height(45.dp),
                    onClick = onNavigateUp
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
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
                    onClick = { onNavigateToWeb("https://www.wanandroid.com/blog/show/2653") }
                ) {
                    Icon(
                        painter = painterResource(R.mipmap.ic_rule),
                        contentDescription = null,
                        tint = colorResource(R.color.white)
                    )
                }
            }
        }
    ) { innerPadding ->
        SwipeRefresh(
            items = uiState.result,
            refreshing = uiState.refreshing,
            loading = uiState.loading,
            finishing = uiState.finishing,
            onRefresh = { viewModel.getHome() },
            onLoad = { viewModel.getNext() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { _, item ->
            Row(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = item.getAvatarId()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onNavigateToUser(item.userId) }
                            .size(30.dp)
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
            Spacer(Modifier.height(1.dp))
        }
    }
}