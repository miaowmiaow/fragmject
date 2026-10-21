package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.image.editor.components.PictureEditorCanvas
import com.example.fragmject.core.image.editor.model.StickerAttrs
import com.example.fragmject.core.image.editor.components.rememberPictureEditorState

@Composable
fun PictureEditorScreen(
    bitmapPath: String? = null,
    bitmapUri: Uri? = null,
    viewModel: PictureEditorViewModel = hiltViewModel(),
    onFinish: (path: String, uri: Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberPictureEditorState()
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val toolbar = rememberEditorToolbarController()

    // 子界面状态
    var showTextSheet by remember { mutableStateOf(false) }
    var showClipScreen by remember { mutableStateOf(false) }
    var clipBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 贴纸选择器：从系统图库选取贴纸图片
    val stickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // 解码下沉到 ImageEditorEngine（IO 线程），不在 ActivityResult 回调里做主线程解码
        uri?.let {
            viewModel.pickSticker(it.toString()) { bitmap ->
                state.setSticker(StickerAttrs(bitmap))
                toolbar.resetSelection()
            }
        }
    }

    LaunchedEffect(bitmapPath, bitmapUri) {
        state.setBitmapPathOrUri(bitmapPath, bitmapUri)
    }

    DisposableEffect(Unit) {
        onDispose { state.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.editorBackground)
    ) {
        // 编辑画布
        PictureEditorCanvas(state = state)

        // 顶部栏
        EditorTopBar(
            modifier = Modifier.align(Alignment.TopCenter),
            isSaving = isSaving,
            onDismiss = onDismiss,
            onSave = {
                if (!isSaving) {
                    triggerEditorSave(viewModel, state, context, onFinish)
                }
            },
        )

        // 底部工具栏
        EditorBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            controller = toolbar,
            editorState = state,
            onRequestSticker = { stickerLauncher.launch("image/*") },
            onRequestText = { showTextSheet = true },
            onRequestClip = {
                clipBitmap = state.saveBitmap()
                showClipScreen = true
            },
        )

        // 文字编辑面板
        if (showTextSheet) {
            PictureTextSheet(
                onFinish = { attrs ->
                    state.setSticker(attrs)
                    showTextSheet = false
                    toolbar.resetSelection()
                },
                onDismiss = { showTextSheet = false }
            )
        }

        // 裁剪子页面
        EditorClipSection(
            bitmap = clipBitmap,
            visible = showClipScreen,
            viewModel = viewModel,
            onFinish = { path, uri ->
                state.setBitmapPathOrUri(path, uri)
                showClipScreen = false
                clipBitmap = null
            },
            onDismiss = {
                showClipScreen = false
                clipBitmap = null
            },
        )

        // 保存中遮罩
        EditorSavingOverlay(visible = isSaving)
    }
}
