package com.example.fragment.project.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage

@Composable
fun <T> Banner(
    data: List<T>?,
    pathMapping: (T) -> String,
    modifier: Modifier = Modifier,
    onClick: (index: Int, item: T) -> Unit
) {
    if (data.isNullOrEmpty()) {
        return
    }
    LoopHorizontalPager(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        data = data,
        indicator = true
    ) { page, pageOffset, item ->
        Card(
            Modifier
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
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = pathMapping(item),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16f))
                    .clickable {
                        onClick(page, item)
                    }
            )
        }
    }
}