package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.designsystem.AppColors

/**
 * 选择页顶栏：返回 / 相册切换 / 确定。
 *
 * 从 [PictureSelectorScreen] 抽出，只为把「相册下拉菜单」这段独立交互与网格渲染解耦。
 */
@Composable
internal fun SelectorTopBar(
    state: SelectorTopBarState,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onExpandMenu: (Boolean) -> Unit,
    onSelectAlbum: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .padding(horizontal = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = AppColors.editorOnBackground
            )
        }

        // 相册选择（始终居中）
        Box(modifier = Modifier.align(Alignment.Center)) {
            Row(
                modifier = Modifier.clickable { onExpandMenu(true) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    state.currentAlbumName.ifEmpty { "相册" },
                    color = AppColors.editorOnBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.editorOnBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = state.menuExpanded,
                onDismissRequest = { onExpandMenu(false) },
                containerColor = AppColors.menuContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                state.albums.forEach { album ->
                    DropdownMenuItem(
                        colors = MenuDefaults.itemColors(
                            textColor = AppColors.editorOnBackground
                        ),
                        text = {
                            Text(
                                "${album.name} (${album.size})",
                                color = AppColors.editorOnBackground
                            )
                        },
                        onClick = { onSelectAlbum(album.name) }
                    )
                }
            }
        }

        TextButton(
            onClick = onConfirm,
            enabled = state.selectedCount > 0,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                if (state.selectedCount == 0) "确定" else "确定(${state.selectedCount})",
                color = if (state.selectedCount > 0) AppColors.blue else Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}
