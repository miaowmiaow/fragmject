package com.example.fragmject.feature.picture.ui.editor

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.example.fragmject.core.image.editor.components.PictureEditorState

/**
 * 触发一次保存：把画布合成、落盘、失败提示这三步从屏幕函数里挪出来。
 *
 * 保存本身由 ViewModel 调度（IO 线程），这里只负责串起回调。
 */
internal fun triggerEditorSave(
    viewModel: PictureEditorViewModel,
    editorState: PictureEditorState,
    context: Context,
    onFinish: (path: String, uri: Uri) -> Unit,
) {
    viewModel.save(
        saveAction = { viewModel.saveBitmap { editorState.saveBitmap() } },
        onSuccess = { result -> onFinish(result.path, result.uri.toUri()) },
        onError = {
            Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
        },
    )
}
