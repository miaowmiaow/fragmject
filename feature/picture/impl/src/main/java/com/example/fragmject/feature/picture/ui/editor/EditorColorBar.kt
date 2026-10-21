package com.example.fragmject.feature.picture.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.picture.utils.ColorUtils

/** 涂鸦颜色选择栏：撤销 + 一组可选颜色。 */
@Composable
internal fun EditorColorBar(onUndo: () -> Unit, onSelectColor: (Int) -> Unit) {
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUndo) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = "撤销",
                tint = AppColors.editorOnBackground
            )
        }
        ColorUtils.colorful.forEachIndexed { index, colorInt ->
            val isSelected = index == selectedColorIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (isSelected) 36.dp else 28.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        if (isSelected) 3.dp else 0.dp,
                        AppColors.editorOnBackground,
                        CircleShape
                    )
                    .clickable {
                        selectedColorIndex = index
                        onSelectColor(colorInt)
                    }
            )
        }
    }
}
