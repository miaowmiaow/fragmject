package com.example.fragment.project.ui.my_coin

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.components.Loading
import com.example.fragment.project.components.SwipeRefresh

@Composable
fun MyCoinScreen(
    viewModel: MyCoinViewModel = viewModel(),
    onNavigateToCoinRank: () -> Unit = {},
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
                text = "我的积分",
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
                    onNavigateToCoinRank()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rank),
                    contentDescription = null,
                    tint = colorResource(R.color.white)
                )
            }
        }
        Box(
            modifier = Modifier
                .background(colorResource(R.color.theme))
                .fillMaxWidth()
                .height(100.dp)
                .wrapContentSize(Alignment.Center)
        ) {
            Text(
                text = uiState.userCoinResult.coinCount,
                fontSize = 60.sp,
                color = colorResource(R.color.text_fff),
            )
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
                data = uiState.myCoinResult,
            ) { _, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = item.getTitle(),
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_666),
                        )
                        Text(
                            text = item.getTime(),
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_999),
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