package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.designsystem.AppColors

/** 预览页顶栏：返回、页码、以及选图流程才出现的「完成」。 */
@Composable
internal fun PreviewTopBar(
    page: Int,
    total: Int,
    showConfirm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.editorBackground.copy(alpha = 0.6f))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = AppColors.editorOnBackground
                )
            }
            Text(
                "${page + 1}/$total",
                color = AppColors.editorOnBackground,
                fontSize = 16.sp
            )
            // 「完成」是选图流程的确认动作；纯看图靠返回箭头关闭，不再显示
            if (showConfirm) {
                TextButton(onClick = onConfirm) {
                    Text("完成", color = AppColors.editorOnBackground, fontSize = 16.sp)
                }
            }
        }
    }
}
