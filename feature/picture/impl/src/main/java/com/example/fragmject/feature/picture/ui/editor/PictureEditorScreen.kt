package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.image.editor.components.PictureEditorCanvas
import com.example.fragmject.core.image.editor.components.PictureEditorState
import com.example.fragmject.core.image.editor.model.StickerAttrs
import com.example.fragmject.core.image.editor.components.rememberPictureEditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()

    // 子界面状态：裁剪位图非空即表示裁剪页可见
    var showTextSheet by remember { mutableStateOf(false) }
    var clipBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // 进入裁剪前要合成全尺寸位图，是 CPU 重活：期间用遮罩挡住交互
    var isPreparingClip by remember { mutableStateOf(false) }

    // 贴纸选择器：从系统图库选取贴纸图片
    val stickerLauncher = rememberStickerPickerLauncher(viewModel, state, toolbar)

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
            onSave = { if (!isSaving) triggerEditorSave(viewModel, state, context, onFinish) },
        )

        // 底部工具栏
        EditorBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            controller = toolbar,
            editorState = state,
            onRequestSticker = { stickerLauncher.launch("image/*") },
            onRequestText = { showTextSheet = true },
            onRequestClip = {
                requestClipBitmap(
                    scope = scope,
                    state = state,
                    isBusy = isSaving,
                    isPreparing = isPreparingClip,
                    onPreparingChange = { isPreparingClip = it },
                    onComposed = { clipBitmap = it },
                )
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
            visible = clipBitmap != null,
            viewModel = viewModel,
            onFinish = { path, uri ->
                state.setBitmapPathOrUri(path, uri)
                clipBitmap = null
            },
            onDismiss = { clipBitmap = null },
        )

        // 保存 / 合成中遮罩：位图合成与编码都在后台线程，遮罩负责挡住期间的所有手势
        EditorSavingOverlay(
            visible = isSaving || isPreparingClip,
            text = if (isPreparingClip) "正在准备裁剪..." else "正在保存...",
        )
    }
}

/** 贴纸选择器：解码下沉到 ImageEditorEngine（IO 线程），不在 ActivityResult 回调里做主线程解码。 */
@Composable
private fun rememberStickerPickerLauncher(
    viewModel: PictureEditorViewModel,
    state: PictureEditorState,
    toolbar: EditorToolbarController,
): ManagedActivityResultLauncher<String, Uri?> =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.pickSticker(it.toString()) { bitmap ->
                state.setSticker(StickerAttrs(bitmap))
                toolbar.resetSelection()
            }
        }
    }

/**
 * 进入裁剪前合成全尺寸位图：大图全尺寸绘制在主线程要上百毫秒，必须放到后台线程，
 * 期间由调用方用遮罩挡住交互。已在保存或已在校准中时直接忽略。
 */
private fun requestClipBitmap(
    scope: CoroutineScope,
    state: PictureEditorState,
    isBusy: Boolean,
    isPreparing: Boolean,
    onPreparingChange: (Boolean) -> Unit,
    onComposed: (Bitmap) -> Unit,
) {
    if (isBusy || isPreparing) return
    onPreparingChange(true)
    scope.launch {
        val composed = withContext(Dispatchers.Default) { state.saveBitmap() }
        onPreparingChange(false)
        if (composed != null) onComposed(composed)
    }
}
