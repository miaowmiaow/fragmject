package com.example.fragment.project.ui.browse_history

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.History
import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

data class BrowseHistoryUiState(
    var bookmarkResult: List<History> = ArrayList(),
    var historyResult: List<History> = ArrayList(),
)

class BrowseHistoryViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(BrowseHistoryUiState())

    val uiState: StateFlow<BrowseHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WanHelper.getBookmark().zip(WanHelper.getBrowseHistory()) { b, h ->
                _uiState.update { state ->
                    state.copy(bookmarkResult = b, historyResult = h)
                }
            }.collect {}
        }
    }

    fun setBrowseHistory(title: String, url: String) {
        viewModelScope.launch {
            WanHelper.setBrowseHistory(title, url)
        }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch {
            WanHelper.deleteHistory(history)
        }
    }

}