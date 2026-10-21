package com.example.fragmject.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.fragmject.core.ui.R

/** 空态的测试标记：空态只有一张图、没有可读文案，UI 测试只能靠标记断言。 */
const val EMPTY_CONTENT_TAG = "content_state_empty"

/**
 * 空态：无数据时展示，不含重试（错误态见 [ErrorContent]）。
 */
@Composable
fun EmptyContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(EMPTY_CONTENT_TAG),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.mipmap.empty),
            contentDescription = null,
        )
    }
}
