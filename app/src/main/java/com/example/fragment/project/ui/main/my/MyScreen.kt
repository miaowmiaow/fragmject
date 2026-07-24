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
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.example.fragment.project.BrowseHistoryNavKey
import com.example.fragment.project.DemoNavKey
import com.example.fragment.project.LoginNavKey
import com.example.fragment.project.MyCoinNavKey
import com.example.fragment.project.MyCollectNavKey
import com.example.fragment.project.MyShareNavKey
import com.example.fragment.project.SettingNavKey
import com.example.fragment.project.UserNavKey
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.ArrowRightItem

@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
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
                    if (uiState.user.id > 0) {
                        onNavigate(UserNavKey(uiState.user.id.toString()))
                    } else {
                        onNavigate(LoginNavKey)
                    }
                }
                .size(90.dp)
        )
        Text(
            text = uiState.user.username.toString().ifBlank { "去登录" },
            modifier = Modifier
                .clickable(
                    onClick = {
                        if (uiState.user.id > 0) {
                            onNavigate(UserNavKey(uiState.user.id.toString()))
                        } else {
                            onNavigate(LoginNavKey)
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
        ArrowRightItem("组件Demo") { onNavigate(DemoNavKey) }
        HorizontalDivider()
        ArrowRightItem("我的积分") { onNavigate(MyCoinNavKey) }
        HorizontalDivider()
        ArrowRightItem("我的收藏") { onNavigate(MyCollectNavKey) }
        HorizontalDivider()
        ArrowRightItem("我的分享") { onNavigate(MyShareNavKey) }
        HorizontalDivider()
        ArrowRightItem("浏览历史") { onNavigate(BrowseHistoryNavKey) }
        HorizontalDivider()
        ArrowRightItem("系统设置") { onNavigate(SettingNavKey) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyScreenPreview() {
    WanTheme { MyScreen() }
}