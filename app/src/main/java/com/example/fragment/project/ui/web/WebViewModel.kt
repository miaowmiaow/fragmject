package com.example.fragment.project.ui.web

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.database.history.History
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WebUiState(
    var bookmarkResult: List<History> = ArrayList(),
) {
    fun getResult(url: String?): History? {
        bookmarkResult.forEach {
            if (it.value == url) {
                return it
            }
        }
        return null
    }
}

class WebViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(WebUiState())

    val uiState: StateFlow<WebUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WanHelper.getBookmark().collect { b ->
                _uiState.update { state ->
                    state.copy(bookmarkResult = b)
                }
            }
        }
    }

    fun setBookmark(url: String) {
        viewModelScope.launch {
            WanHelper.setBookmark(url)
        }
    }

    fun setBrowseHistory(url: String) {
        viewModelScope.launch {
            WanHelper.setBrowseHistory(url)
        }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch {
            WanHelper.deleteHistory(history)
        }
    }

}