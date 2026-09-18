package com.example.fragmject.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 非列表内容的首屏骨架屏：「标题条 + 通栏条目条」。
 * 每个骨架块独立带 shimmer 扫光。
 */
@Composable
fun SkeletonScreen(
    modifier: Modifier = Modifier,
    rowCount: Int = 8,
    contentPadding: PaddingValues = PaddingValues(10.dp),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 页面标题占位
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(28.dp)
        )
        Spacer(Modifier.height(10.dp))
        repeat(rowCount) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}
