package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.model.MediaItem

/** 选择页正常态：顶栏 + 四列网格。 */
@Composable
internal fun SelectorContent(
    topBarState: SelectorTopBarState,
    items: List<MediaItem>,
    selectedUriSet: Set<String>,
    selectPositionMap: Map<String, Int>,
    actions: SelectorActions,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.editorBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        SelectorTopBar(
            state = topBarState,
            onBack = actions.onBack,
            onConfirm = actions.onConfirm,
            onExpandMenu = actions.onExpandMenu,
            onSelectAlbum = actions.onSelectAlbum,
        )

        SelectorGrid(
            items = items,
            selectedUriSet = selectedUriSet,
            selectPositionMap = selectPositionMap,
            onTakePicture = actions.onTakePicture,
            onToggleSelection = actions.onToggleSelection,
        )
    }
}
