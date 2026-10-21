package com.example.fragmject.core.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random

/**
 * 掘金上看到 clwater 写的效果很棒，非常适合用来做暗夜模式
 * 基于此封装成 NightSwitchButton 方便使用
 * 文章地址：https://juejin.cn/post/7225454746949615673
 */
@SuppressLint("UnusedBoxWithConstraintsScope", "FrequentlyChangingValue")
@Composable
fun NightSwitchButton(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight
        val canvasRadius = canvasHeight / 2f
        val minBound = 0f
        val maxBound = with(LocalDensity.current) { (canvasWidth * 0.5f).toPx() }
        var forceAnimationCheck by remember { mutableStateOf(false) }
        val anchoredDraggableState = remember(maxBound) {
            AnchoredDraggableState(
                initialValue = checked,
                anchors = DraggableAnchors {
                    false at minBound
                    true at maxBound
                },
            )
        }
        val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
        val currentChecked by rememberUpdatedState(checked)
        LaunchedEffect(anchoredDraggableState) {
            snapshotFlow { anchoredDraggableState.currentValue }
                .collectLatest { newValue ->
                    if (currentChecked != newValue) {
                        currentOnCheckedChange?.invoke(newValue)
                        forceAnimationCheck = !forceAnimationCheck
                    }
                }
        }
        LaunchedEffect(checked, forceAnimationCheck) {
            if (checked != anchoredDraggableState.currentValue) {
                anchoredDraggableState.animateTo(checked)
            }
        }
        Box(
            modifier = Modifier
                .anchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Horizontal,
                    enabled = onCheckedChange != null,
                )
        ) {
            val progress = anchoredDraggableState.offset / maxBound
            val sharedTransition = rememberInfiniteTransition()
            Sky(progress, canvasWidth, canvasHeight, canvasRadius, sharedTransition)
            Cloud(progress, canvasWidth, canvasHeight, canvasRadius, sharedTransition)
            Stars(progress, canvasWidth, canvasHeight, canvasRadius)
            SunAndMoon(progress, canvasWidth, canvasHeight, canvasRadius, sharedTransition)
        }
    }
}

