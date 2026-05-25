package com.example.fragment.project.ui.share

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.repository.MyRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareArticleUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val message: String = "",
)

class ShareArticleViewModel(
    private val myRepo: MyRepository = WanRepositoryProvider.my,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ShareArticleUiState())

    val uiState: StateFlow<ShareArticleUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.update {
            it.copy(message = "")
        }
    }

    fun share(title: String, link: String) {
        _uiState.update {
            it.copy(isLoading = true)
        }
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            val response = myRepo.shareArticle(title, link)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    success = response.errorCode == "0",
                    message = response.errorMsg
                )
            }
        }
    }
}