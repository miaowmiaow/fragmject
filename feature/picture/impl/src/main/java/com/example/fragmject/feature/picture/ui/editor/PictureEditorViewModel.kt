package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.image.editor.ImageEditorEngine
import com.example.fragmject.core.image.editor.SaveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 图片编辑 / 裁剪保存 ViewModel。
 *
 * 保存编排与平台位图解耦（T-14 方案 2）：UI 通过 [save] 传入「合成 + 落盘」动作，
 * ViewModel 只负责防重复点击、`isSaving` 状态与结果分发。
 * 位图的生成与编码仍由 [ImageEditorEngine] 承担，不在 ViewModel 内持有 `Bitmap`。
 */
@HiltViewModel
class PictureEditorViewModel @Inject constructor(
    private val imageEditorEngine: ImageEditorEngine,
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /**
     * 保存编辑结果。
     *
     * [saveAction] 由 UI 提供完整保存动作（返回 null 表示失败），例如：
     * `saveAction = { saveBitmap { editorState.saveBitmap() } }`。
     * 这样 ViewModel 不接收 `produce: () -> Bitmap?`，JVM 单测可用任意挂起 lambda
     * 覆盖成功、失败与取消，无需构造 `Bitmap`。
     */
    @Suppress("SwallowedException") // 异常统一转成 onError 交给 UI 提示，无需额外日志
    fun save(
        saveAction: suspend () -> SaveResult?,
        onSuccess: (SaveResult) -> Unit,
        onError: () -> Unit,
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val result = saveAction()
                if (result != null) onSuccess(result) else onError()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError()
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** 合成位图并交给引擎落盘；供 UI 在 [save] 的 [saveAction] 内组合使用。 */
    suspend fun saveBitmap(produce: () -> Bitmap?): SaveResult? =
        produce()?.let { imageEditorEngine.save(it) }

    /** 贴纸选取：解码下沉到 [ImageEditorEngine]，不在 ActivityResult 回调里主线程解码。 */
    fun pickSticker(uriString: String, targetWidth: Int = 512, onLoaded: (Bitmap) -> Unit) {
        viewModelScope.launch {
            imageEditorEngine.loadBitmap(null, uriString, targetWidth)?.let(onLoaded)
        }
    }
}
