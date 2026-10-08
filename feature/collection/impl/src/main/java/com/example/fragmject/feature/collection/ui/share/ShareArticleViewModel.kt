package com.example.fragmject.feature.collection.ui.share

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.example.fragmject.core.ui.utils.updateSuccessFrom
import com.example.fragmject.core.domain.usecase.ShareArticleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShareArticleUiState {
    data class Content(
        val isLoading: Boolean = false,
        val success: Boolean = false,
        val message: String = "",
        val title: String = "",
        val link: String = "",
    ) : ShareArticleUiState
}

val ShareArticleUiState.isLoading get() = (this as? ShareArticleUiState.Content)?.isLoading ?: false
val ShareArticleUiState.success get() = (this as? ShareArticleUiState.Content)?.success ?: false
val ShareArticleUiState.message get() = (this as? ShareArticleUiState.Content)?.message ?: ""
val ShareArticleUiState.title get() = (this as? ShareArticleUiState.Content)?.title ?: ""
val ShareArticleUiState.link get() = (this as? ShareArticleUiState.Content)?.link ?: ""

@HiltViewModel
class ShareArticleViewModel @Inject constructor(
    private val shareArticle: ShareArticleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareArticleUiState>(ShareArticleUiState.Content())

    val uiState: StateFlow<ShareArticleUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.updateSuccessFrom({ ShareArticleUiState.Content() }) { it.copy(message = "")
        }
    }

    fun updateTitle(title: String) {
        _uiState.updateSuccessFrom({ ShareArticleUiState.Content() }) { it.copy(title = title) }
    }

    fun updateLink(link: String) {
        _uiState.updateSuccessFrom({ ShareArticleUiState.Content() }) { it.copy(link = link) }
    }

    /**
     * 提交分享：表单校验（标题 / 链接判空）下沉到 ViewModel，
     * 校验失败时通过 [ShareArticleUiState.message] 反馈，由 UI 层 Snackbar 展示。
     */
    fun submit() {
        val current = _uiState.value as? ShareArticleUiState.Content ?: return
        if (current.title.isBlank()) {
            _uiState.updateSuccessFrom({ ShareArticleUiState.Content() }) {
                it.copy(message = "文章标题不能为空")
            }
            return
        }
        if (current.link.isBlank()) {
            _uiState.updateSuccessFrom({ ShareArticleUiState.Content() }) {
                it.copy(message = "文章链接不能为空")
            }
            return
        }
        _uiState.updateSuccessFrom({ ShareArticleUiState.Content() }) { it.copy(isLoading = true) }
        viewModelScope.launch {
            val response = shareArticle(current.title, current.link)
            _uiState.updateSuccessFrom({ ShareArticleUiState.Content() }) {
                if (response.success) {
                    // 成功：显式置 success=true，空消息回退默认文案
                    it.copy(
                        isLoading = false,
                        success = true,
                        message = response.message.ifBlank { "分享成功" },
                    )
                } else {
                    it.copy(isLoading = false, success = false, message = response.message)
                }
            }
        }
    }
}