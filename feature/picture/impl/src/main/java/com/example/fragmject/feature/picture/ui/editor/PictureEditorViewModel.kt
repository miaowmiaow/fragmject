package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.image.editor.ImageEditorEngine
import com.example.fragmject.core.image.editor.SaveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 图片编辑 / 裁剪保存 ViewModel。
 *
 * 经由 [ImageEditorEngine] 完成「位图加载 + 有效性校验 + 编码 + 落盘」，
 * UI 不再直接调用平台解码/保存能力，也不再自行判断位图是否有效。
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
     * [produce] 返回画布合成出的位图（未就绪时返回 null），
     * 之后的校验、编码、落盘全部由 [ImageEditorEngine] 承担。
     */
    fun save(
        produce: () -> Bitmap?,
        onSuccess: (SaveResult) -> Unit,
        onError: () -> Unit,
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val bitmap = produce()
                val result = if (bitmap == null) null else imageEditorEngine.save(bitmap)
                if (result != null) onSuccess(result) else onError()
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** 贴纸选取：解码下沉到 [ImageEditorEngine]，不在 ActivityResult 回调里主线程解码。 */
    fun pickSticker(uriString: String, targetWidth: Int = 512, onLoaded: (Bitmap) -> Unit) {
        viewModelScope.launch {
            imageEditorEngine.loadBitmap(null, uriString, targetWidth)?.let(onLoaded)
        }
    }
}
