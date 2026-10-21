package com.example.fragmject.feature.home.ui.my

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.model.User
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.contract.Navigators
import com.example.fragmject.core.ui.components.ArrowRightItem
import com.example.fragmject.core.ui.utils.AvatarUtils

@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
) {
    val navigators = LocalNavigators.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(45.dp))
        MyProfileHeader(user = uiState.user, navigators = navigators)
        Spacer(Modifier.height(45.dp))
        MyMenu(navigators)
    }
}

/** 头像 + 昵称；未登录时统一走登录入口。 */
@Composable
private fun MyProfileHeader(user: User, navigators: Navigators) {
    // 有 id 视为已登录：点头像/昵称进个人主页，否则去登录
    val onProfileClick: () -> Unit = {
        if (user.id > 0) {
            navigators.user.openUserProfile(user.id.toString())
        } else {
            navigators.auth.openLogin()
        }
    }
    MyAvatar(user = user, onClick = onProfileClick)
    Text(
        text = user.username.toString().ifBlank { "去登录" },
        modifier = Modifier
            .clickable(
                onClick = onProfileClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .height(45.dp)
            .padding(10.dp),
        fontSize = 16.sp,
        textAlign = TextAlign.Center
    )
}

/** 头像：有远程图标用 AsyncImage，否则用本地随机头像。 */
@Composable
private fun MyAvatar(user: User, onClick: () -> Unit) {
    val modifier = Modifier
        .clip(RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .size(90.dp)
    val icon = user.icon
    if (!icon.isNullOrBlank()) {
        AsyncImage(
            model = icon,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = AvatarUtils.avatarResId(user.id)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

/** 上方菜单项；顺序即展示顺序，末项之后不再画分隔线。 */
@Composable
private fun MyMenu(navigators: Navigators) {
    val items = remember(navigators) {
        listOf(
            "组件Demo" to { navigators.demo.openDemo() },
            "我的积分" to { navigators.user.openMyCoin() },
            "我的收藏" to { navigators.collection.openMyCollect() },
            "我的分享" to { navigators.collection.openMyShare() },
            "浏览历史" to { navigators.user.openBrowseHistory() },
            "身份验证器" to { navigators.authenticator.openAuthenticator() },
            "系统设置" to { navigators.user.openSetting() },
        )
    }
    items.forEachIndexed { index, (title, action) ->
        ArrowRightItem(title) { action() }
        if (index != items.lastIndex) {
            HorizontalDivider()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MyScreenPreview() {
    AppTheme { MyScreen() }
}
