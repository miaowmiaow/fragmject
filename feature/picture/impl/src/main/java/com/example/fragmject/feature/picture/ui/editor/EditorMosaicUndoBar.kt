package com.example.fragmject.feature.picture.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppColors

/** 马赛克撤销栏。 */
@Composable
internal fun EditorMosaicUndoBar(onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onUndo) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = "撤销马赛克",
                tint = AppColors.editorOnBackground
            )
        }
    }
}
