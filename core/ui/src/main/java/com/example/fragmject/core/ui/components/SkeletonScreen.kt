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
 *
 * 整屏只创建一个 shimmer 动画（[rememberShimmerProgress]），所有骨架块共享进度并各自带
 * 相位偏移（[shimmerPhaseOf]）保留错落感。
 */
@Composable
fun SkeletonScreen(
    modifier: Modifier = Modifier,
    rowCount: Int = 8,
    contentPadding: PaddingValues = PaddingValues(10.dp),
) {
    val shimmerProgress = rememberShimmerProgress()
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
                .height(28.dp),
            shimmerProgress = shimmerProgress,
            shimmerPhase = shimmerPhaseOf(0),
        )
        Spacer(Modifier.height(10.dp))
        repeat(rowCount) { rowIndex ->
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shimmerProgress = shimmerProgress,
                shimmerPhase = shimmerPhaseOf(rowIndex + 1),
            )
        }
    }
}
