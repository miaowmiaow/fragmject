package com.example.fragmject.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.example.fragmject.core.ui.components.LoopHorizontalPager

@Composable
fun <T> BannerPager(
    data: List<T>?,
    pathMapping: (T) -> String,
    modifier: Modifier = Modifier,
    onClick: (index: Int, item: T) -> Unit
) {
    if (data.isNullOrEmpty()) {
        return
    }
    // 预计算图片 URL 列表：自动轮播动画每帧都会重新执行 content lambda，
    // 若在每帧重复调用 pathMapping(item) 会产生不必要的调用与 String 实例分配。
    // 图片 URL 只随 data 变化，故以 data 为 key 缓存即可。
    val imageUrls = remember(data) { data.map(pathMapping) }
    LoopHorizontalPager(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        data = data,
        indicator = true
    ) { page, pageOffset, item ->
        AsyncImage(
            model = imageUrls[page],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .graphicsLayer {
                    lerp(
                        start = 0.95f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleX = scale
                        scaleY = scale
                    }
                }
                .clip(RoundedCornerShape(5.dp))
                .clipToBounds()
                .clickable { onClick(page, item) }
                .fillMaxSize()
                .aspectRatio(16f / 9f)
        )
    }
}