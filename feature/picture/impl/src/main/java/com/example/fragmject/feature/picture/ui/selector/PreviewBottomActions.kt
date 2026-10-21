package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.designsystem.AppColors

/** 预览页底部操作：编辑入口 +（选图流程下的）选择框。 */
@Composable
internal fun PreviewBottomActions(
    editable: Boolean,
    showSelection: Boolean,
    selected: Boolean,
    onEdit: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 编辑按钮：纯看图时不渲染（网络图也走不了本地编辑链路）
        if (editable) {
            TextButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑",
                    tint = AppColors.editorOnBackground
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("编辑", color = AppColors.editorOnBackground, fontSize = 14.sp)
            }
        }

        // 选择框（仅选图流程；纯看图不该改动选图结果）
        if (showSelection) {
            TextButton(onClick = onToggleSelection) {
                SelectionMark(isSelected = selected)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selected) "取消选择" else "选择",
                    color = AppColors.editorOnBackground,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/** 圆圈选择标记：选中填充蓝色并显示对勾。 */
@Composable
private fun SelectionMark(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, AppColors.editorOnBackground, CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(AppColors.blue, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = AppColors.editorOnBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
