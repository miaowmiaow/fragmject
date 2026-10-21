package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.fragmject.feature.picture.model.MediaItem

/** 预览页单张图：手势缩放 + 按当前缩放/位移做变换。 */
@Composable
internal fun PreviewPage(
    media: MediaItem?,
    zoomState: PageZoomState,
    onToggleBars: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .previewGestures(zoomState = zoomState, onToggleBars = onToggleBars),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = media?.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.offsetX
                    translationY = zoomState.offsetY
                }
        )
    }
}
