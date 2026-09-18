package com.example.fragmject.feature.collection.ui.share

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.domain.usecase.ShareArticleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShareArticleUiState {
    data class Success(
        val isLoading: Boolean = false,
        val success: Boolean = false,
        val message: String = "",
        val title: String = "",
        val link: String = "",
    ) : ShareArticleUiState
}

val ShareArticleUiState.isLoading get() = (this as? ShareArticleUiState.Success)?.isLoading ?: false
val ShareArticleUiState.success get() = (this as? ShareArticleUiState.Success)?.success ?: false
val ShareArticleUiState.message get() = (this as? ShareArticleUiState.Success)?.message ?: ""
val ShareArticleUiState.title get() = (this as? ShareArticleUiState.Success)?.title ?: ""
val ShareArticleUiState.link get() = (this as? ShareArticleUiState.Success)?.link ?: ""

@HiltViewModel
class ShareArticleViewModel @Inject constructor(
    private val shareArticle: ShareArticleUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ShareArticleUiState>(ShareArticleUiState.Success())

    val uiState: StateFlow<ShareArticleUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.updateSuccessFrom({ ShareArticleUiState.Success() }) { it.copy(message = "")
        }
    }

    fun updateTitle(title: String) {
        _uiState.updateSuccessFrom({ ShareArticleUiState.Success() }) { it.copy(title = title) }
    }

    fun updateLink(link: String) {
        _uiState.updateSuccessFrom({ ShareArticleUiState.Success() }) { it.copy(link = link) }
    }

    /**
     * 提交分享：表单校验（标题 / 链接判空）下沉到 ViewModel，
     * 校验失败时通过 [ShareArticleUiState.message] 反馈，由 UI 层 Snackbar 展示。
     */
    fun submit() {
        val current = _uiState.value as? ShareArticleUiState.Success ?: return
        if (current.title.isBlank()) {
            _uiState.updateSuccessFrom({ ShareArticleUiState.Success() }) {
                it.copy(message = "文章标题不能为空")
            }
            return
        }
        if (current.link.isBlank()) {
            _uiState.updateSuccessFrom({ ShareArticleUiState.Success() }) {
                it.copy(message = "文章链接不能为空")
            }
            return
        }
        _uiState.updateSuccessFrom({ ShareArticleUiState.Success() }) { it.copy(isLoading = true) }
        viewModelScope.launch {
            val response = shareArticle(current.title, current.link)
            _uiState.updateSuccessFrom({ ShareArticleUiState.Success() }) {
                it.copy(isLoading = false, success = response.success, message = response.message)
            }
        }
    }
}