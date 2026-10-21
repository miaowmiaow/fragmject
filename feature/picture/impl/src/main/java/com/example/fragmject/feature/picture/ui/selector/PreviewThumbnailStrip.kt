package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.model.MediaItem

/** 选图流程的底部缩略图条：未选中的图覆盖半透明遮罩。 */
@Composable
internal fun PreviewThumbnailStrip(
    items: List<MediaItem>,
    selectedUriSet: Set<String>,
    onSelect: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(items, key = { _, item -> item.uri }) { index, media ->
            val isSelected = media.uri.toString() in selectedUriSet
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        2.dp,
                        if (isSelected) AppColors.blue else Color.Transparent
                    )
                    .clickable { onSelect(index) }
            ) {
                AsyncImage(
                    model = media.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.editorOnBackground.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
