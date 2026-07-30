package com.example.fragmject.feature.wan.browse_history

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.database.model.HistoryEntity
import com.example.fragmject.core.database.store.HistoryStore
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BrowseHistoryUiState {
    data class Success(
        val bookmarkResult: List<HistoryEntity> = emptyList(),
        val historyResult: List<HistoryEntity> = emptyList(),
    ) : BrowseHistoryUiState
}

// Screen accessors
val BrowseHistoryUiState.bookmarkResult: List<HistoryEntity> get() = (this as? BrowseHistoryUiState.Success)?.bookmarkResult ?: emptyList()
val BrowseHistoryUiState.historyResult: List<HistoryEntity> get() = (this as? BrowseHistoryUiState.Success)?.historyResult ?: emptyList()

private inline fun MutableStateFlow<BrowseHistoryUiState>.updateData(
    crossinline block: (BrowseHistoryUiState.Success) -> BrowseHistoryUiState.Success,
) {
    update { if (it is BrowseHistoryUiState.Success) block(it) else it }
}

@HiltViewModel
class BrowseHistoryViewModel @Inject constructor() : BaseViewModel() {

    private val _uiState = MutableStateFlow<BrowseHistoryUiState>(BrowseHistoryUiState.Success())
    val uiState: StateFlow<BrowseHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            HistoryStore.getBookmark().zip(HistoryStore.getBrowseHistory()) { b, h ->
                _uiState.updateData { it.copy(bookmarkResult = b, historyResult = h) }
            }.collect {}
        }
    }

    fun setBrowseHistory(title: String, url: String) {
        viewModelScope.launch { HistoryStore.setBrowseHistory(title, url) }
    }

    fun deleteHistory(history: HistoryEntity) {
        viewModelScope.launch { HistoryStore.deleteHistory(history) }
    }
}