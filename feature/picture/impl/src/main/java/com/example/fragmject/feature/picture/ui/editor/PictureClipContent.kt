package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.image.editor.components.PictureClipCanvas
import com.example.fragmject.core.image.editor.components.rememberPictureClipState

@Composable
fun PictureClipContent(
    bitmap: Bitmap,
    viewModel: PictureEditorViewModel,
    onFinish: (path: String, uri: Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberPictureClipState()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(bitmap) {
        state.setBitmap(bitmap)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.editorBackground)
    ) {
        // 裁剪画布
        PictureClipCanvas(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 140.dp)
        )

        // 顶部工具栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.editorBackground.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.systemBars)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = AppColors.editorOnBackground, fontSize = 16.sp)
                }
                Row {
                    IconButton(onClick = { state.rotate() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "旋转",
                            tint = AppColors.editorOnBackground
                        )
                    }
                    IconButton(onClick = { state.reset() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "重置",
                            tint = AppColors.editorOnBackground
                        )
                    }
                }
                TextButton(
                    onClick = {
                        if (!isSaving) {
                            val bitmap = state.saveBitmap()
                            if (bitmap == null) {
                                Toast.makeText(context, "图片加载失败，请重试", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.save(
                                    produce = { bitmap },
                                    onSuccess = { result -> onFinish(result.path, result.uri.toUri()) },
                                    onError = {
                                        Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
                                    },
                                )
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("确定", color = AppColors.editorOnBackground, fontSize = 16.sp)
                }
            }
        }

        // 底部操作提示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.editorBackground.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.systemBars)
                .align(Alignment.BottomCenter)
        ) {
            Text(
                "拖动边框调整裁剪区域",
                modifier = Modifier.padding(16.dp),
                color = AppColors.editorOnBackground.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        // 保存中遮罩
        AnimatedVisibility(visible = isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.editorBackground.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        trackColor = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在保存...", color = AppColors.editorOnBackground)
                }
            }
        }
    }
}