package com.example.fragmject.feature.picture.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.media.EditedImage
import com.example.fragmject.core.domain.media.ImageHandle
import com.example.fragmject.core.domain.media.ImageSource
import com.example.fragmject.core.domain.media.MediaEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 图片编辑 / 裁剪保存 ViewModel。
 *
 * 经由 [MediaEditor] 领域端口完成「位图加载 + 有效性校验 + 编码 + 落盘」，
 * UI 不再直接调用平台解码/保存能力，也不再自行判断位图是否有效。
 */
@HiltViewModel
class PictureEditorViewModel @Inject constructor(
    private val mediaEditor: MediaEditor,
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveProgress = MutableStateFlow(0f)
    val saveProgress: StateFlow<Float> = _saveProgress.asStateFlow()

    /**
     * 保存编辑结果。
     *
     * [produce] 返回画布合成出的位图句柄（未就绪时返回 null），
     * 之后的校验、编码、落盘全部由 [MediaEditor] 承担。
     */
    fun save(
        produce: () -> ImageHandle?,
        onSuccess: (EditedImage) -> Unit,
        onError: () -> Unit,
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val handle = produce()
                val edited = if (handle == null) null else mediaEditor.saveImage(handle)
                if (edited != null) onSuccess(edited) else onError()
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** 贴纸选取：解码下沉到 [MediaEditor]，不在 ActivityResult 回调里主线程解码。 */
    fun pickSticker(uriString: String, targetWidth: Int = 512, onLoaded: (ImageHandle) -> Unit) {
        viewModelScope.launch {
            mediaEditor.loadBitmap(ImageSource.Uri(uriString), targetWidth)?.let(onLoaded)
        }
    }

    /** 供画布加载位图：返回领域句柄，解码由 [MediaEditor] 在 IO 线程完成。 */
    suspend fun loadBitmap(source: ImageSource, targetWidth: Int = 0): ImageHandle? =
        mediaEditor.loadBitmap(source, targetWidth)
}
