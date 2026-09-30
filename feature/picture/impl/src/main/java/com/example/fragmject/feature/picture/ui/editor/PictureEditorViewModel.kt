package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * 图片编辑 / 裁剪保存 ViewModel。
 *
 * 承接原先生硬写在 `PictureEditorScreen` / `PictureClipScreen` 中的保存编排
 * （Bitmap 压缩 + MediaRepository 落盘 + isSaving 状态），
 * 使 `mediaRepository` 不再作为参数穿透两个 Composable 层。
 */
@HiltViewModel
class PictureEditorViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** 将 Bitmap 压缩为 PNG 并保存到相册；成功后回传 path/uri，失败回调 [onError]。 */
    fun save(
        bitmap: Bitmap,
        onSuccess: (path: String, uri: Uri) -> Unit,
        onError: () -> Unit,
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                baos.toByteArray()
            }
            val result = mediaRepository.saveImageToAlbum(bytes)
            _isSaving.value = false
            // 仅保存成功且 URI 有效才回调成功；失败则保留编辑页并提示，
            // 避免后续以空 URI 替换/删除原图。
            if (result.success && result.uri.isNotBlank()) {
                onSuccess(result.path, result.uri.toUri())
            } else {
                onError()
            }
        }
    }
}
