package com.example.fragmject.feature.wan.share

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.data.repository.MyRepository
import com.example.fragmject.core.domain.usecase.ShareArticleUseCase
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShareArticleUiState {
    data class Success(
val isLoading: Boolean = false,
    val success: Boolean = false,
    val message: String = "",
    ) : ShareArticleUiState
}

val ShareArticleUiState.isLoading get() = (this as? ShareArticleUiState.Success)?.isLoading ?: false
val ShareArticleUiState.success get() = (this as? ShareArticleUiState.Success)?.success ?: false
val ShareArticleUiState.message get() = (this as? ShareArticleUiState.Success)?.message ?: ""

private inline fun MutableStateFlow<ShareArticleUiState>.updateData(
    crossinline block: (ShareArticleUiState.Success) -> ShareArticleUiState.Success,
) {
    update { if (it is ShareArticleUiState.Success) block(it) else it }
}
@HiltViewModel
class ShareArticleViewModel @Inject constructor(
    private val shareArticle: ShareArticleUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ShareArticleUiState>(ShareArticleUiState.Success())

    val uiState: StateFlow<ShareArticleUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.updateData { it.copy(message = "")
        }
    }

    fun share(title: String, link: String) {
        _uiState.updateData { it.copy(isLoading = true) }
        viewModelScope.launch {
            val response = shareArticle(title, link)
            _uiState.updateData {
                it.copy(isLoading = false, success = response.success, message = response.message)
            }
        }
    }
}