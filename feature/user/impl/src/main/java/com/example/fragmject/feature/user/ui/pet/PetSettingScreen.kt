package com.example.fragmject.feature.user.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.TitleBar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 漂浮宠物配置页：开关 + 透明度 / 大小 / 游动速度三个滑杆。
 *
 * ============================ 为什么没有预览框 ============================
 * 不需要：宠物本身就被 `:app` 挂在组合根、浮在本页之上，改完参数松手后
 * **屏幕上那只就是实时预览**。再放一个预览框反而会在同一屏出现两条鱼。
 *
 * ============================ 写入时机 ============================
 * 滑杆只在**松手时**落库（`onValueChangeFinished`），拖动过程中只改本地状态。
 * 否则一次拖动会写几十次 KV，每次写入又经 Room Flow 回推、触发重组，滑杆会发涩。
 */
@Composable
fun PetSettingScreen(
    viewModel: PetSettingViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val onNavigateUp = LocalOnNavigateUp.current

    Scaffold(
        topBar = {
            TitleBar(
                title = "漂浮宠物",
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            PetSwitchRow(
                label = "显示宠物",
                checked = settings.enabled,
                onCheckedChange = viewModel::setEnabled,
            )
            HorizontalDivider()

            PetSliderRow(
                label = "透明度",
                value = settings.opacity,
                valueRange = PetSettings.MIN_OPACITY..PetSettings.MAX_OPACITY,
                display = "${(settings.opacity * 100).roundToInt()}%",
                onCommit = viewModel::setOpacity,
            )
            HorizontalDivider()

            PetSliderRow(
                label = "大小",
                value = settings.sizeScale,
                valueRange = PetSettings.MIN_SIZE_SCALE..PetSettings.MAX_SIZE_SCALE,
                display = formatScale(settings.sizeScale),
                onCommit = viewModel::setSizeScale,
            )
            HorizontalDivider()

            PetSliderRow(
                label = "游动速度",
                value = settings.speedScale,
                valueRange = PetSettings.MIN_SPEED_SCALE..PetSettings.MAX_SPEED_SCALE,
                display = formatScale(settings.speedScale),
                onCommit = viewModel::setSpeedScale,
            )

            Text(
                text = "调整后松手即刻生效，屏幕上那只就是实时预览。宠物浮在所有页面之上，" +
                    "只观察触摸、不拦截点击与滚动；关闭后连帧循环一起停止。",
                modifier = Modifier.padding(horizontal = 25.dp, vertical = 16.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun PetSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .height(45.dp)
            .padding(horizontal = 25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 带本地拖动态的滑杆。
 *
 * [LaunchedEffect] 同步的是「外部已落库的值」：拖动中外部值不变，所以不会和手指打架；
 * 松手提交后外部值回流，再把本地值对齐一次（防止提交被夹紧后显示与库内不一致）。
 *
 * 用旧重载并显式 suppress：替代品（`SliderState` + `Slider(state = ...)`）在当前
 * Material3 版本里还不存在（`Slider` 没有 `state` 参数），依赖升级后再迁移。
 */
@Suppress("DEPRECATION")
@Composable
private fun PetSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    display: String,
    onCommit: (Float) -> Unit,
) {
    var current by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { current = value }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text(text = display, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onCommit(current) },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 显示成 `×1.2` 这类倍率，避免把 0.6~1.8 的裸浮点直接怼给用户。
 *
 * 显式传 [Locale.ROOT]：默认 locale 在阿拉伯语等环境会用不同数字字形，倍率显示会跳变。
 */
private fun formatScale(scale: Float): String = "×%.1f".format(Locale.ROOT, scale)

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun PetSettingScreenPreview() {
    AppTheme { PetSettingScreen() }
}
