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
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.fragmject.core.domain.text.FontScale
import com.example.fragmject.core.ui.components.TitleBar

/**
 * 设置行内开关的统一几何：两种开关（自绘的 [NightSwitchButton] 与系统 `Switch`）都按
 * 52×32 绘制、距行尾 25dp 收边，这样「深色模式」和「用登录态缓存图片」两行开关的
 * 轨道位置与宽度完全一致，不会出现左右错位。
 */
private val SettingSwitchWidth = 52.dp
private val SettingSwitchHeight = 32.dp
private val SettingSwitchEndPadding = 25.dp

@Composable
fun SettingScreen(
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val articleNavigator = LocalNavigators.current.article
    val homeNavigator = LocalNavigators.current.home
    val userNavigator = LocalNavigators.current.user
    val onNavigateUp = LocalOnNavigateUp.current
    val cacheSize by viewModel.cacheSizeText.collectAsStateWithLifecycle()
    val isClearing by viewModel.isClearing.collectAsStateWithLifecycle()
    val credentialedCacheEnabled by viewModel.credentialedCacheEnabled.collectAsStateWithLifecycle()
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
                SettingItems(
                    innerPadding = innerPadding,
                    uiState = uiState,
                    cacheSize = cacheSize,
                    isClearing = isClearing,
                    credentialedCacheEnabled = credentialedCacheEnabled,
                    actions = SettingActions(
                        onDarkThemeChange = viewModel::updateDarkTheme,
                        onBodyFontSpChange = viewModel::updateBodyFontSp,
                        onCredentialedCacheChange = viewModel::setCredentialedCacheEnabled,
                        onPetSettingClick = userNavigator::openPetSetting,
                        onClearCacheClick = { showDialog = true },
                        onArticleClick = articleNavigator::openArticle,
                        onLogoutClick = viewModel::logout,
                    ),
                )
            }
        }
    )
}

/** 设置项的行内动作集合：收敛成 1 个参数，避免私有 composable 的参数列表超限。 */
private class SettingActions(
    val onDarkThemeChange: (Boolean) -> Unit,
    val onBodyFontSpChange: (Float) -> Unit,
    val onCredentialedCacheChange: (Boolean) -> Unit,
    val onPetSettingClick: () -> Unit,
    val onClearCacheClick: () -> Unit,
    val onArticleClick: (String) -> Unit,
    val onLogoutClick: () -> Unit,
)

/** 设置项列表：拆出来后 [SettingScreen] 只负责 Scaffold / 事件收集 / 弹窗。 */
@Composable
private fun SettingItems(
    innerPadding: PaddingValues,
    uiState: SettingUiState,
    cacheSize: String,
    isClearing: Boolean,
    credentialedCacheEnabled: Boolean,
    actions: SettingActions,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SettingSwitchRow(
            label = "深色模式",
            checked = uiState.darkTheme,
            onCheckedChange = actions.onDarkThemeChange,
        )
        HorizontalDivider()
        // 字体大小：与深色模式同属「显示」类设置，放在一起；滑杆松手才提交（见 SettingFontScaleRow）
        SettingFontScaleRow(
            bodyFontSp = uiState.bodyFontSp,
            onCommit = actions.onBodyFontSpChange,
        )
        HorizontalDivider()
        // 登录态缓存：默认开启（体验优先）。关闭后只缓存匿名内容，并清掉已落盘的带凭据条目，
        // 代价仅为「需要登录才能取到的图片」不再进缓存，展示走原生请求不受影响。
        SettingStandardSwitchRow(
            label = "用登录态缓存图片",
            checked = credentialedCacheEnabled,
            onCheckedChange = actions.onCredentialedCacheChange,
        )
        HorizontalDivider()
        // 宠物配置单独一页：三个滑杆 + 实时预览，塞进设置页会让这一屏过长
        ArrowRightItem(title = "漂浮宠物", onClick = actions.onPetSettingClick)
        HorizontalDivider()
        ArrowRightItem("隐私政策") {
            actions.onArticleClick("file:///android_asset/html/privacy_policy.html")
        }
        HorizontalDivider()
        ArrowRightItem("问题反馈") {
            actions.onArticleClick("https://github.com/miaowmiaow/fragmject/issues")
        }
        HorizontalDivider()
        SettingCacheRow(
            cacheSize = cacheSize,
            isClearing = isClearing,
            onClick = actions.onClearCacheClick,
        )
        HorizontalDivider()
        ArrowRightItem("关于玩Android") {
            actions.onArticleClick("https://wanandroid.com")
        }
        Spacer(Modifier.height(20.dp))
        if (uiState.user != null) {
            SettingLogoutButton(onClick = actions.onLogoutClick)
        }
    }
}

/**
 * 设置项：左标题 + 右侧**系统** [Switch]。
 *
 * 与深色模式用的 [NightSwitchButton] 区分：后者是带太阳/月亮自绘动画的主题专用控件，
 * 只在「切主题」这一项需要；其余设置项用标准 `Switch`，状态语义、无障碍朗读与
 * Material3 主题适配都由系统组件承担，不需要为每一项重复实现动画。
 */
@Composable
private fun SettingStandardSwitchRow(
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // 与 NightSwitchButton 共用同一套尺寸/右边距，保证两行开关视觉对齐
            modifier = Modifier
                .padding(end = SettingSwitchEndPadding)
                .size(width = SettingSwitchWidth, height = SettingSwitchHeight),
        )
    }
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
            modifier = Modifier
                .padding(end = SettingSwitchEndPadding)
                .size(width = SettingSwitchWidth, height = SettingSwitchHeight),
        )
    }
}

/**
 * 设置项：字体大小滑块（13~18sp，1sp 步进）。
 *
 * 拖动中只改本地值，松手才提交 —— 避免每移动一像素就往 Room 写一次；
 * 全局生效点是 `MainActivity` 的 `AppTheme(fontScale = …)`，它改的是
 * `LocalDensity.fontScale`（只放大文字，dp 尺寸不变）。
 *
 * 不写死行高：滑块本身约 48dp，字号调大后也不该被裁（对照规则 28 的最小高度约定）。
 */
// 旧重载被标记 deprecated，但其替代品（`SliderState` + `Slider(state = ...)`）在当前
// Material3 版本里还不存在：`Slider` 没有 `state` 参数、`SliderState` 也没有 `valueRange`。
// 依赖升级后再迁移；届时顺手把 `remember(bodyFontSp)` 换成 `LaunchedEffect` 同步。
@Suppress("DEPRECATION")
@Composable
private fun SettingFontScaleRow(
    bodyFontSp: Float,
    onCommit: (Float) -> Unit,
) {
    var current by remember(bodyFontSp) { mutableFloatStateOf(bodyFontSp) }
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingRowLabel("字体大小")
            Text(
                text = "${current.toInt()}sp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 25.dp),
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onCommit(current) },
            valueRange = FontScale.MIN_BODY_SP..FontScale.MAX_BODY_SP,
            // 13~20 共 8 档，1sp 步进：steps = 中间档位数 = 总档位 - 2
            steps = (FontScale.MAX_BODY_SP - FontScale.MIN_BODY_SP).toInt() - 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
        )
        // 日历是密集表格（方形单元格 + 圆形裁剪），叠加应用内倍率会被裁；
        // 它只跟随系统字体。写在界面上，避免用户以为设置失灵。
        Text(
            text = "日历等密集表格跟随系统字体，不随此处变化",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 25.dp)
                .padding(bottom = 8.dp),
        )
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