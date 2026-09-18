package com.example.fragmject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 非列表内容加载包装器：isLoading 为 true 时显示居中的 [CircularProgressIndicator]，
 * 否则渲染 [content]。API 与 [SkeletonContent] 对齐，供不需要骨架屏、只需
 * 简单转圈提示的页面使用（如登录/注册/设置/分享文章）。
 */
@Composable
fun LoadingContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit,
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
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
