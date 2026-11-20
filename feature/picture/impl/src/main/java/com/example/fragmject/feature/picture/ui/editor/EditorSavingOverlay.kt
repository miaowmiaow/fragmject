package com.example.fragmject.feature.picture.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppColors

/**
 * 保存/合成中的半透明遮罩，**真正阻断**编辑期交互。
 *
 * 只加背景不会成为触摸命中目标，手势会穿透到底下的工具栏；而位图合成与编码已经移到后台线程
 * （见 `PictureEditorViewModel.saveBitmap`），此时若还能点按钮，就会与后台线程并发读写编辑器
 * 状态（图层栈、变换矩阵）。因此这里显式消费所有指针事件。
 */
@Composable
internal fun EditorSavingOverlay(visible: Boolean, text: String = "正在保存...") {
    AnimatedVisibility(visible = visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.editorBackground.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    trackColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text, color = AppColors.editorOnBackground)
            }
        }
    }
}
