package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/** 预览页每一张图独立的缩放与平移状态。 */
@Stable
internal class PageZoomState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
}
