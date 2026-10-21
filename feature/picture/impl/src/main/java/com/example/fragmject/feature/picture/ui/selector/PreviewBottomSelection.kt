package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.runtime.Immutable

/** 底部区域的两个开关与当前页选中态。 */
@Immutable
internal data class PreviewBottomSelection(
    val editable: Boolean,
    val showThumbnails: Boolean,
    val currentSelected: Boolean,
)
