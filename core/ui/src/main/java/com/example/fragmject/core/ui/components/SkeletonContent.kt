package com.example.fragmject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 非列表内容加载包装器：isLoading 为 true 时显示 [SkeletonScreen] 骨架，
 * 否则渲染 [content]。参数与旧的 LoadingContent 保持一致，便于平滑替换；
 * 视觉从「覆盖转圈」改为「骨架屏」，统一加载体验。
 */
@Composable
fun SkeletonContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit,
) {
    if (isLoading) {
        SkeletonScreen(
            modifier = modifier,
            contentPadding = innerPadding,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
