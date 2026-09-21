package com.example.fragmject.feature.user.ui.history

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.History
import com.example.fragmject.core.domain.repository.HistoryRepository
import androidx.lifecycle.ViewModel
import com.example.fragmject.core.ui.utils.updateSuccessFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BrowseHistoryUiState {
    data class Success(
        val bookmarkResult: List<History> = emptyList(),
        val historyResult: List<History> = emptyList(),
    ) : BrowseHistoryUiState
}

// Screen accessors
val BrowseHistoryUiState.bookmarkResult: List<History> get() = (this as? BrowseHistoryUiState.Success)?.bookmarkResult ?: emptyList()
val BrowseHistoryUiState.historyResult: List<History> get() = (this as? BrowseHistoryUiState.Success)?.historyResult ?: emptyList()

@HiltViewModel
class BrowseHistoryViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BrowseHistoryUiState>(BrowseHistoryUiState.Success())
    val uiState: StateFlow<BrowseHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepo.observeBookmarks().zip(historyRepo.observeBrowseHistory()) { b, h ->
                _uiState.updateSuccessFrom({ BrowseHistoryUiState.Success() }) { it.copy(bookmarkResult = b, historyResult = h) }
            }.collect {}
        }
    }

    fun setBrowseHistory(title: String, url: String) {
        viewModelScope.launch { historyRepo.setBrowseHistory(title, url) }
    }

    fun deleteHistory(history: History) {
        viewModelScope.launch { historyRepo.deleteHistory(history) }
    }
}