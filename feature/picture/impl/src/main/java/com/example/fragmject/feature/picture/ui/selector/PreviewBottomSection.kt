package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.model.MediaItem

/**
 * 预览页底部区域：缩略图条 + 操作栏。
 *
 * 纯看图的 NORM 模式整条都不渲染，避免留下一条空黑条。
 */
@Composable
internal fun PreviewBottomSection(
    visible: Boolean,
    items: List<MediaItem>,
    selectedUriSet: Set<String>,
    selection: PreviewBottomSelection,
    onSelectThumbnail: (Int) -> Unit,
    onEdit: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.editorBackground.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selection.showThumbnails) {
                    PreviewThumbnailStrip(
                        items = items,
                        selectedUriSet = selectedUriSet,
                        onSelect = onSelectThumbnail,
                    )
                }

                PreviewBottomActions(
                    editable = selection.editable,
                    showSelection = selection.showThumbnails,
                    selected = selection.currentSelected,
                    onEdit = onEdit,
                    onToggleSelection = onToggleSelection,
                )
            }
        }
    }
}
