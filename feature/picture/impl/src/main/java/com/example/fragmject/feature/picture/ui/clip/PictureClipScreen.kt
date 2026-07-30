package com.example.fragmject.feature.picture.ui.clip

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.network.utils.saveImagesToAlbum
import com.example.fragmject.feature.picture.components.PictureClipCanvas
import com.example.fragmject.feature.picture.components.rememberPictureClipState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PictureClipScreen(
    bitmap: Bitmap,
    onFinish: (path: String, uri: Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberPictureClipState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(bitmap) {
        state.setBitmap(bitmap)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                .background(Color.Black.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.systemBars)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = Color.White, fontSize = 16.sp)
                }
                Row {
                    IconButton(onClick = { state.rotate() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "旋转",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { state.reset() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "重置",
                            tint = Color.White
                        )
                    }
                }
                TextButton(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    state.saveBitmap()
                                }
                                context.saveImagesToAlbum(result) { path, uri ->
                                    onFinish(path, uri)
                                }
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("确定", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // 底部操作提示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.systemBars)
                .align(Alignment.BottomCenter)
        ) {
            Text(
                "拖动边框调整裁剪区域",
                modifier = Modifier.padding(16.dp),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        // 保存中遮罩
        AnimatedVisibility(visible = isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在保存...", color = Color.White)
                }
            }
        }
    }
}