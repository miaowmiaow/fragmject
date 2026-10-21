package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable

/** 裁剪子页面：仅在 [bitmap] 非空且 [visible] 时渲染。 */
@Composable
internal fun EditorClipSection(
    bitmap: Bitmap?,
    visible: Boolean,
    viewModel: PictureEditorViewModel,
    onFinish: (path: String, uri: Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    if (bitmap == null || !visible) return
    PictureClipContent(
        bitmap = bitmap,
        viewModel = viewModel,
        onFinish = onFinish,
        onDismiss = onDismiss,
    )
}
