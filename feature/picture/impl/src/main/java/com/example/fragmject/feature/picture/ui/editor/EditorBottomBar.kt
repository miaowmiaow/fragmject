package com.example.fragmject.feature.picture.ui.editor

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
import com.example.fragmject.core.image.editor.components.PictureEditorState

/** 编辑器底部区域：颜色栏 / 马赛克撤销栏 / 工具栏，按当前选中工具显隐。 */
@Composable
internal fun EditorBottomBar(
    modifier: Modifier = Modifier,
    controller: EditorToolbarController,
    editorState: PictureEditorState,
    onRequestSticker: () -> Unit,
    onRequestText: () -> Unit,
    onRequestClip: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.editorBackground.copy(alpha = 0.6f))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = controller.showColorBar) {
                EditorColorBar(
                    onUndo = editorState::graffitiUndo,
                    onSelectColor = editorState::setGraffitiColor,
                )
            }

            AnimatedVisibility(visible = controller.showMosaicUndo) {
                EditorMosaicUndoBar(onUndo = editorState::mosaicUndo)
            }

            EditorToolRow(
                controller = controller,
                editorState = editorState,
                onRequestSticker = onRequestSticker,
                onRequestText = onRequestText,
                onRequestClip = onRequestClip,
            )
        }
    }
}
