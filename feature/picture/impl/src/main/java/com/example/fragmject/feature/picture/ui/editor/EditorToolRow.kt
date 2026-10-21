package com.example.fragmject.feature.picture.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.image.editor.components.EditorMode
import com.example.fragmject.core.image.editor.components.PictureEditorState

/** 工具栏项：名称 + 图标，顺序即 [EditorToolRow] 里的 index 语义。 */
private val TOOLS: List<Pair<String, ImageVector>> = listOf(
    "涂鸦" to Icons.Filled.Create,
    "贴纸" to Icons.Filled.EmojiEmotions,
    "文字" to Icons.Filled.TextFields,
    "裁剪" to Icons.Filled.Crop,
    "马赛克" to Icons.Filled.GridOn,
)

/**
 * 底部工具栏：五个工具按钮。
 *
 * 点击语义：再次点击当前工具 = 收起；否则切换到该工具并收起其它附加栏。
 * 贴纸/文字/裁剪这三项需要外部配合（拉起图库、弹面板、开子页），故通过回调上抛。
 */
@Composable
internal fun EditorToolRow(
    controller: EditorToolbarController,
    editorState: PictureEditorState,
    onRequestSticker: () -> Unit,
    onRequestText: () -> Unit,
    onRequestClip: () -> Unit,
) {
    fun onToolClick(index: Int) {
        val isSelected = index == controller.selectedToolIndex
        controller.showColorBar = false
        controller.showMosaicUndo = false
        if (isSelected) {
            controller.selectedToolIndex = -1
            editorState.setMode(EditorMode.STICKER)
            return
        }
        controller.selectedToolIndex = index
        when (index) {
            0 -> {
                controller.showColorBar = true
                editorState.setMode(EditorMode.GRAFFITI)
            }

            1 -> onRequestSticker()

            2 -> onRequestText()

            3 -> onRequestClip()

            4 -> {
                controller.showMosaicUndo = true
                editorState.setMode(EditorMode.MOSAIC)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TOOLS.forEachIndexed { index, (name, icon) ->
            val isSelected = index == controller.selectedToolIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToolClick(index) }
            ) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = if (isSelected) AppColors.blue else AppColors.editorOnBackground,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    name,
                    color = if (isSelected) AppColors.blue else AppColors.editorOnBackground,
                    fontSize = 11.sp
                )
            }
        }
    }
}
