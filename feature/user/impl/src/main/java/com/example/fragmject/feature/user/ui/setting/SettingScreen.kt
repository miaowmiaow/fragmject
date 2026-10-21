package com.example.fragmject.feature.user.ui.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.fragmject.core.ui.R
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.ArrowRightItem
import com.example.fragmject.core.ui.components.LoadingContent
import com.example.fragmject.core.ui.components.NightSwitchButton
import com.example.fragmject.core.ui.components.StandardDialog
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.ui.components.TitleBar

@Composable
fun SettingScreen(
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val articleNavigator = LocalNavigators.current.article
    val homeNavigator = LocalNavigators.current.home
    val onNavigateUp = LocalOnNavigateUp.current
    val cacheSize by viewModel.cacheSizeText.collectAsStateWithLifecycle()
    val isClearing by viewModel.isClearing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                // 登出成功：经首页语义动作清栈回首页（HomeNavigator.openMain 的实际使用点）
                SettingEvent.LogoutSucceeded -> homeNavigator.openMain()
                SettingEvent.LogoutFailed -> snackbarHostState.showSnackbar("登出失败，请重试")
                is SettingEvent.CacheClearFailed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    var showDialog by remember { mutableStateOf(false) }
    StandardDialog(
        show = showDialog,
        title = "提示",
        text = "确定要清除缓存吗？",
        onConfirm = {
            viewModel.clearCache()
            showDialog = false
        },
        onDismiss = { showDialog = false },
    )
    Scaffold(
        topBar = {
            TitleBar(
                title = "设置",
                onNavigateUp = onNavigateUp,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        content = { innerPadding ->
            LoadingContent(uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SettingSwitchRow(
                        label = "深色模式",
                        checked = uiState.darkTheme,
                        onCheckedChange = viewModel::updateDarkTheme,
                    )
                    HorizontalDivider()
                    ArrowRightItem("隐私政策") {
                        articleNavigator.openArticle("file:///android_asset/html/privacy_policy.html")
                    }
                    HorizontalDivider()
                    ArrowRightItem("问题反馈") {
                        articleNavigator.openArticle("https://github.com/miaowmiaow/fragmject/issues")
                    }
                    HorizontalDivider()
                    SettingCacheRow(
                        cacheSize = cacheSize,
                        isClearing = isClearing,
                        onClick = { showDialog = true },
                    )
                    HorizontalDivider()
                    ArrowRightItem("关于玩Android") {
                        articleNavigator.openArticle("https://wanandroid.com")
                    }
                    Spacer(Modifier.height(20.dp))
                    if (uiState.user != null) {
                        SettingLogoutButton(onClick = viewModel::logout)
                    }
                }
            }
        }
    )
}

/** 设置项：左标题 + 右侧开关。 */
@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingRowLabel(label)
        NightSwitchButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(52.dp, 32.dp)
        )
        Spacer(Modifier.width(5.dp))
    }
}

/** 设置项：左标题 + 右侧缓存大小，点击弹确认框。 */
@Composable
private fun SettingCacheRow(
    cacheSize: String,
    isClearing: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(enabled = !isClearing, onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingRowLabel("清除缓存")
        Text(
            text = if (isClearing) "清理中…" else cacheSize,
            fontSize = 13.sp,
        )
        Image(
            painter = painterResource(id = R.mipmap.ic_right),
            contentDescription = "",
            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
        )
    }
}

/** 设置项左侧标题，占满剩余宽度。 */
@Composable
private fun RowScope.SettingRowLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .weight(1f)
            .padding(start = 25.dp, end = 25.dp),
        fontSize = 13.sp,
    )
}

/** 退出登录按钮。 */
@Composable
private fun SettingLogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, AppColors.theme),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(0.dp, 15.dp, 0.dp, 15.dp)
    ) {
        Text(
            text = "退出登录",
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SettingScreenPreview() {
    AppTheme { SettingScreen() }
}