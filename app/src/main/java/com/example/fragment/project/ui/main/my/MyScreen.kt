package com.example.fragment.project.ui.main.my

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.ArrowRightItem

@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel(),
    onNavigateToBookmarkHistory: () -> Unit = {},
    onNavigateToDemo: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToMyCoin: () -> Unit = {},
    onNavigateToMyCollect: () -> Unit = {},
    onNavigateToMyShare: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(45.dp))
        AsyncImage(
            model = uiState.user.avatar,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable {
                    if (uiState.user.id.isNotBlank()) {
                        onNavigateToUser(uiState.user.id)
                    } else {
                        onNavigateToLogin()
                    }
                }
                .size(90.dp)
        )
        Text(
            text = uiState.user.username.ifBlank { "去登录" },
            modifier = Modifier
                .clickable(
                    onClick = {
                        if (uiState.user.id.isNotBlank()) {
                            onNavigateToUser(uiState.user.id)
                        } else {
                            onNavigateToLogin()
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .height(45.dp)
                .padding(10.dp),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(45.dp))
        ArrowRightItem("组件Demo") { onNavigateToDemo() }
        HorizontalDivider()
        ArrowRightItem("我的积分") { onNavigateToMyCoin() }
        HorizontalDivider()
        ArrowRightItem("我的收藏") { onNavigateToMyCollect() }
        HorizontalDivider()
        ArrowRightItem("我的分享") { onNavigateToMyShare() }
        HorizontalDivider()
        ArrowRightItem("浏览历史") { onNavigateToBookmarkHistory() }
        HorizontalDivider()
        ArrowRightItem("系统设置") { onNavigateToSetting() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyScreenPreview() {
    WanTheme { MyScreen() }
}