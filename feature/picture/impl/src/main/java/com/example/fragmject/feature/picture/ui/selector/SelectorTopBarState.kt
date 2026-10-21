package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.runtime.Immutable
import com.example.fragmject.feature.picture.model.Album

/** 选择页顶栏的展示数据：当前相册名、相册列表、菜单展开态、已选数量。 */
@Immutable
internal data class SelectorTopBarState(
    val currentAlbumName: String,
    val albums: List<Album>,
    val menuExpanded: Boolean,
    val selectedCount: Int,
)
