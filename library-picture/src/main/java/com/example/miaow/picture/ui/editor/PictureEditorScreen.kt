package com.example.miaow.picture.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miaow.base.utils.getBitmapFromUri
import com.example.miaow.base.utils.saveImagesToAlbum
import com.example.miaow.picture.components.EditorMode
import com.example.miaow.picture.components.PictureEditorCanvas
import com.example.miaow.picture.components.rememberPictureEditorState
import com.example.miaow.picture.data.StickerAttrs
import com.example.miaow.picture.ui.clip.PictureClipScreen
import com.example.miaow.picture.utils.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PictureEditorScreen(
    bitmapPath: String? = null,
    bitmapUri: Uri? = null,
    onFinish: (path: String, uri: Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberPictureEditorState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var showColorBar by remember { mutableStateOf(false) }
    var showMosaicUndo by remember { mutableStateOf(false) }
    var selectedToolIndex by remember { mutableIntStateOf(-1) }
    var isSaving by remember { mutableStateOf(false) }

    // 子界面状态
    var showTextSheet by remember { mutableStateOf(false) }
    var showClipScreen by remember { mutableStateOf(false) }
    var clipBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 贴纸选择器：从系统图库选取贴纸图片
    val stickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = context.getBitmapFromUri(it, 512)
            bmp?.let { bitmap ->
                state.setSticker(StickerAttrs(bitmap))
                selectedToolIndex = -1
            }
        }
    }

    LaunchedEffect(bitmapPath, bitmapUri) {
        state.setBitmapPathOrUri(bitmapPath, bitmapUri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 编辑画布
        PictureEditorCanvas(state = state)

        // 顶部栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                TextButton(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                val result = withContext(Dispatchers.Default) { state.saveBitmap() }
                                context.saveImagesToAlbum(result) { path, uri ->
                                    isSaving = false
                                    onFinish(path, uri)
                                }
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("完成", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // 底部工具栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 颜色选择栏
                AnimatedVisibility(visible = showColorBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { state.graffitiUndo() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "撤销",
                                tint = Color.White
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
                                        Color.White,
                                        CircleShape
                                    )
                                    .clickable {
                                        selectedColorIndex = index
                                        state.setGraffitiColor(colorInt)
                                    }
                            )
                        }
                    }
                }

                // 马赛克撤销
                AnimatedVisibility(visible = showMosaicUndo) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { state.mosaicUndo() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "撤销马赛克",
                                tint = Color.White
                            )
                        }
                    }
                }

                // 工具选择栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val toolNames = remember { listOf("涂鸦", "贴纸", "文字", "裁剪", "马赛克") }
                    val toolIcons = remember {
                        listOf(
                            Icons.Filled.Create,
                            Icons.Filled.EmojiEmotions,
                            Icons.Filled.TextFields,
                            Icons.Filled.Crop,
                            Icons.Filled.GridOn,
                        )
                    }

                    toolNames.forEachIndexed { index, name ->
                        val isSelected = index == selectedToolIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (isSelected) {
                                    selectedToolIndex = -1
                                    showColorBar = false
                                    showMosaicUndo = false
                                    state.setMode(EditorMode.STICKER)
                                } else {
                                    selectedToolIndex = index
                                    when (index) {
                                        0 -> {
                                            showColorBar = !showColorBar
                                            showMosaicUndo = false
                                            state.setMode(EditorMode.GRAFFITI)
                                        }
                                        1 -> {
                                            showColorBar = false
                                            showMosaicUndo = false
                                            stickerLauncher.launch("image/*")
                                        }
                                        2 -> {
                                            showColorBar = false
                                            showMosaicUndo = false
                                            showTextSheet = true
                                        }
                                        3 -> {
                                            showColorBar = false
                                            showMosaicUndo = false
                                            clipBitmap = state.saveBitmap()
                                            showClipScreen = true
                                        }
                                        4 -> {
                                            showColorBar = false
                                            showMosaicUndo = !showMosaicUndo
                                            state.setMode(EditorMode.MOSAIC)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                toolIcons[index],
                                contentDescription = name,
                                tint = if (isSelected) Color(0xFF508CEE) else Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                name,
                                color = if (isSelected) Color(0xFF508CEE) else Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 文字编辑面板
        if (showTextSheet) {
            PictureTextSheet(
                onFinish = { attrs ->
                    state.setSticker(attrs)
                    showTextSheet = false
                    selectedToolIndex = -1
                },
                onDismiss = { showTextSheet = false }
            )
        }

        // 裁剪子页面
        clipBitmap?.let { bmp ->
            if (showClipScreen) {
                PictureClipScreen(
                    bitmap = bmp,
                    onFinish = { path, uri ->
                        state.setBitmapPathOrUri(path, uri)
                        showClipScreen = false
                        clipBitmap = null
                    },
                    onDismiss = {
                        showClipScreen = false
                        clipBitmap = null
                    }
                )
            }
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