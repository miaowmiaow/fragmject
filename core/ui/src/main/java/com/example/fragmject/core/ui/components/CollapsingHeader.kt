package com.example.fragmject.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 折叠头部状态：封装「头部高度动画 + 嵌套滚动连接」，
 * 供需要折叠头部的页面（MyCoinScreen / UserScreen 等）复用。
 */
@Stable
class CollapsingHeaderState(
    val titleBarSize: Dp,
    val targetHeight: Dp,
    val titleBarSizePx: Float,
    val targetHeightPx: Float,
    val targetPercent: Animatable<Float, AnimationVector1D>,
    val nestedScrollConnection: NestedScrollConnection,
)

/**
 * 创建折叠头部状态。默认标题栏 45dp、目标展开高度 100dp。
 */
@Composable
fun rememberCollapsingHeaderState(
    titleBarSize: Dp = 45.dp,
    targetHeight: Dp = 100.dp,
): CollapsingHeaderState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val titleBarSizePx = with(density) { titleBarSize.roundToPx().toFloat() }
    val targetHeightPx = with(density) { targetHeight.roundToPx().toFloat() }
    val targetPercent = remember { Animatable(1f) }
    val nestedScrollConnection = remember(scope, targetHeightPx, targetPercent) {
        createCollapsingNestedScrollConnection(scope, targetHeightPx, targetPercent)
    }
    return remember(
        titleBarSize,
        targetHeight,
        titleBarSizePx,
        targetHeightPx,
        targetPercent,
        nestedScrollConnection,
    ) {
        CollapsingHeaderState(
            titleBarSize = titleBarSize,
            targetHeight = targetHeight,
            titleBarSizePx = titleBarSizePx,
            targetHeightPx = targetHeightPx,
            targetPercent = targetPercent,
            nestedScrollConnection = nestedScrollConnection,
        )
    }
}

private fun createCollapsingNestedScrollConnection(
    scope: CoroutineScope,
    targetHeightPx: Float,
    targetPercent: Animatable<Float, AnimationVector1D>,
): NestedScrollConnection = object : NestedScrollConnection {
    var dyConsumed = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        dyConsumed += delta
        dyConsumed = dyConsumed.coerceAtMost(0f)
        val percent = dyConsumed / targetHeightPx
        scope.launch {
            val targetValue = 1 - abs(percent.coerceIn(-1f, 0f))
            if (targetValue != targetPercent.value) {
                targetPercent.animateTo(targetValue)
            }
        }
        return if (percent > -1 && percent < 0) Offset(0f, delta) else Offset.Zero
    }
}

/**
 * 折叠头部容器：背景 + 状态栏 padding + 高度动画 + 返回按钮 + 自定义内容。
 *
 * @param actions 右上角操作区（在 [BoxScope] 内，需自行 align）
 * @param content 居中自定义内容（在 [BoxScope] 内，需自行 align）
 */
@Composable
fun CollapsingHeader(
    state: CollapsingHeaderState,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(AppColors.theme)
            .statusBarsPadding()
            .fillMaxWidth()
            .height(state.titleBarSize + state.targetHeight * state.targetPercent.value)
    ) {
        IconButton(
            modifier = Modifier.height(state.titleBarSize),
            onClick = onNavigateUp,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        actions()
        content()
    }
}
