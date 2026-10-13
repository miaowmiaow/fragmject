package com.example.fragmject.feature.collection.ui.share

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.example.fragmject.core.domain.repository.MyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareArticleUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val message: String = "",
    val title: String = "",
    val link: String = "",
)

@HiltViewModel
class ShareArticleViewModel @Inject constructor(
    private val myRepository: MyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareArticleUiState())

    val uiState: StateFlow<ShareArticleUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.update { it.copy(message = "") }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateLink(link: String) {
        _uiState.update { it.copy(link = link) }
    }

    /**
     * 提交分享：表单校验（标题 / 链接判空）下沉到 ViewModel，
     * 校验失败时通过 [ShareArticleUiState.message] 反馈，由 UI 层 Snackbar 展示。
     */
    fun submit() {
        val current = _uiState.value
        if (current.title.isBlank()) {
            _uiState.update { it.copy(message = "文章标题不能为空") }
            return
        }
        if (current.link.isBlank()) {
            _uiState.update { it.copy(message = "文章链接不能为空") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val response = myRepository.shareArticle(current.title, current.link)
            _uiState.update {
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
